import PropTypes from 'prop-types';
import React, { Component } from 'react';
import classnames from 'classnames';

import { getAttributesInstance } from '../../../actions/AppActions';
import {
  completeRequest,
  initLayout,
  patchRequest,
} from '../../../actions/GenericActions';
import { parseToDisplay } from '../../../actions/WindowActions';
import AttributesDropdown from './AttributesDropdown';

export default class Attributes extends Component {
  constructor(props) {
    super(props);

    this.state = {
      dropdown: false,
      layout: null,
      data: null,
    };
  }

  handleInit = () => {
    const {
      docType,
      dataId,
      tabId,
      rowId,
      fieldName,
      attributeType,
      widgetData,
      entity,
    } = this.props;

    const tmpId = widgetData.value.key;

    getAttributesInstance(
      attributeType,
      tmpId,
      docType,
      dataId,
      tabId,
      rowId,
      fieldName,
      entity
    )
      .then(response => {
        const { id, fieldsByName } = response.data;

        this.setState({
          data: parseToDisplay(fieldsByName),
        });

        return initLayout(attributeType, id);
      })
      .then(response => {
        const { elements } = response.data;

        this.setState({
          layout: elements,
        });
      })
      .then(() => {
        this.setState({
          dropdown: true,
        });
      });
  };

  handleToggle = option => {
    const { handleBackdropLock } = this.props;

    this.setState(
      {
        data: null,
        layout: null,
        dropdown: null,
      },
      () => {
        //Method is disabling outside click in parents
        //elements if there is some
        handleBackdropLock && handleBackdropLock(!!option);

        if (option) {
          this.handleInit();
        }
      }
    );
  };

  handleKeyDown = e => {
    switch (e.key) {
      case 'Escape':
        e.preventDefault();
        this.handleCompletion();
        break;
    }
  };

  handleChange = (field, value) => {
    this.setState(prevState => ({
      data: Object.assign({}, prevState.data, {
        [field]: Object.assign({}, prevState.data[field], { value }),
      }),
    }));
  };

  handlePatch = (prop, value, id, cb) => {
    const { attributeType } = this.props;

    return patchRequest({
      entity: attributeType,
      docType: null,
      docId: id,
      property: prop,
      value,
    }).then(response => {
      if (response.data && response.data.length) {
        const fields = response.data[0].fieldsByName;
        Object.keys(fields).map(fieldName => {
          this.setState(
            prevState => ({
              data: Object.assign({}, prevState.data, {
                [fieldName]: Object.assign({}, prevState.data[fieldName], {
                  value,
                }),
              }),
            }),
            () => cb && cb()
          );
        });
      }
    });
  };

  handleCompletion = () => {
    const { data } = this.state;
    const mandatory = Object.keys(data).filter(
      fieldName => data[fieldName].mandatory
    );
    const valid = !mandatory.filter(field => !data[field].value).length;

    //there are required values that are not set. just close
    if (mandatory.length && !valid) {
      if (window.confirm('Do you really want to leave?')) {
        this.handleToggle(false);
      }
      return;
    }

    this.doCompleteRequest();
    this.handleToggle(false);
  };

  doCompleteRequest = () => {
    const { attributeType, patch } = this.props;
    const { data } = this.state;
    const attrId = data && data.ID ? data.ID.value : -1;

    completeRequest(attributeType, attrId).then(response => {
      patch(response.data);
    });
  };

  render() {
    const {
      widgetData,
      dataId,
      rowId,
      attributeType,
      tabIndex,
      readonly,
    } = this.props;

    const { dropdown, data, layout } = this.state;

    const { value } = widgetData;
    const label = value.caption;
    const attrId = data && data.ID ? data.ID.value : -1;

    return (
      <div
        onKeyDown={this.handleKeyDown}
        className={classnames('attributes', {
          'attributes-in-table': rowId,
        })}
      >
        <button
          tabIndex={tabIndex}
          onClick={() => this.handleToggle(true)}
          className={classnames(
            'btn btn-block tag tag-lg tag-block tag-secondary pointer',
            {
              'tag-disabled': dropdown,
              'tag-disabled disabled': readonly,
            }
          )}
        >
          {label ? label : 'Edit'}
        </button>
        {dropdown && (
          <AttributesDropdown
            attributeType={attributeType}
            dataId={dataId}
            tabIndex={tabIndex}
            onClickOutside={this.handleCompletion}
            data={data}
            layout={layout}
            handlePatch={this.handlePatch}
            handleChange={this.handleChange}
            attrId={attrId}
          />
        )}
      </div>
    );
  }
}

Attributes.propTypes = {
  patch: PropTypes.func,
};

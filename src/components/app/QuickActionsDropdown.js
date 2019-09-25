import React, { Component } from 'react';
import onClickOutside from 'react-onclickoutside';
import { connect } from 'react-redux';

/**
 * @file Class based component.
 * @module QuickActionsDropdown
 * @extends Component
 */
class QuickActionsDropdown extends Component {
  constructor(props) {
    super(props);
  }

  /**
   * @method handleClickOutside
   * @summary ToDo: Describe the method
   * @todo Write the documentation
   */
  handleClickOutside = () => {
    const { handleClickOutside } = this.props;

    handleClickOutside();
  };

  /**
   * @method componentDidMount
   * @summary ToDo: Describe the method
   * @todo Write the documentation
   */
  componentDidMount() {
    document.getElementsByClassName('quick-actions-item')[0].focus();
  }

  /**
   * @method handleKeyDown
   * @summary ToDo: Describe the method
   * @param {*} event
   * @param {*} action
   * @todo Write the documentation
   */
  handleKeyDown = (e, action) => {
    const { handleClick } = this.props;
    e.preventDefault();
    const next = document.activeElement.nextSibling;
    const prev = document.activeElement.previousSibling;
    switch (e.key) {
      case 'ArrowDown':
        if (!document.activeElement.classList.contains('quick-actions-item')) {
          document.getElementsByClassName('quick-actions-item')[0].focus();
        } else {
          if (next && next.classList.contains('quick-actions-item')) {
            next.focus();
          }
        }

        break;
      case 'ArrowUp':
        if (prev && prev.classList.contains('quick-actions-item')) {
          prev.focus();
        }
        break;
      case 'Enter':
        handleClick(action);
        break;
    }
  };

  /**
   * @method handleItem
   * @summary ToDo: Describe the method
   * @param {*} item
   * @todo Write the documentation
   */
  handleItem = item => {
    document.getElementsByClassName('quick-actions-item')[item].focus();
  };

  /**
   * @method render
   * @summary ToDo: Describe the method
   * @todo Write the documentation
   */
  render() {
    const { actions, handleClick } = this.props;

    return (
      <div className="quick-actions-dropdown">
        {actions.map((action, index) => (
          <div
            id={`quickAction_${
              action.internalName ? action.internalName : action.processId
            }`}
            tabIndex={0}
            ref={c => (this.item = c)}
            className={
              'quick-actions-item ' +
              (action.disabled ? 'quick-actions-item-disabled ' : '')
            }
            key={index}
            onClick={() => handleClick(action)}
            onKeyDown={e => this.handleKeyDown(e, action)}
            onMouseEnter={() => this.handleItem(index)}
          >
            {action.caption}
            {action.disabled && (
              <p className="one-line">
                <small>({action.disabledReason})</small>
              </p>
            )}
          </div>
        ))}
      </div>
    );
  }
}

export default connect()(onClickOutside(QuickActionsDropdown));

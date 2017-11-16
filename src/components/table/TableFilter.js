import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import counterpart from 'counterpart';
import TableQuickInput from './TableQuickInput';
import Tooltips from '../tooltips/Tooltips';
import keymap from '../../keymap.js';

class TableFilter extends Component {
    constructor(props) {
        super(props);

        this.state = {
            isTooltipShow: false
        }
    }

    toggleTooltip = (key = null) => {
        this.setState({
            isTooltipShow: key
        });
    }

    render() {
        const {
            openModal, toggleFullScreen, fullScreen, docType, docId, tabId,
            isBatchEntry, handleBatchEntryToggle, supportQuickInput,
            allowCreateNew
        } = this.props;

        const {
            isTooltipShow
        } = this.state;

        return (
            <div className="form-flex-align table-filter-line">
                <div className="form-flex-align">
                    <div>
                        {(!isBatchEntry && allowCreateNew) && <button
                            className="btn btn-meta-outline-secondary btn-distance btn-sm"
                            onClick={openModal}
                            tabIndex="-1"
                        >
                            {counterpart.translate('window.addNew.caption')}
                        </button>}
                        {(supportQuickInput && !fullScreen && allowCreateNew) &&
                            <button
                                className="btn btn-meta-outline-secondary btn-distance btn-sm"
                                onClick={handleBatchEntryToggle}
                                onMouseEnter={() =>
                                    this.toggleTooltip(
                                        keymap.TABLE_CONTEXT.TOGGLE_QUICK_INPUT
                                    )
                                }
                                onMouseLeave={this.toggleTooltip}
                                tabIndex="-1"
                            >
                            {isBatchEntry ? counterpart.translate(
                                'window.batchEntryClose.caption'
                            ) : counterpart.translate(
                                'window.batchEntry.caption'
                            )}
                            {isTooltipShow ===
                                keymap.TABLE_CONTEXT.TOGGLE_QUICK_INPUT &&
                                <Tooltips
                                    name={
                                        keymap.TABLE_CONTEXT.TOGGLE_QUICK_INPUT
                                    }
                                    action={
                                        isBatchEntry ?
                                            counterpart.translate(
                                                'window.batchEntryClose.caption'
                                            ) :
                                            counterpart.translate(
                                                'window.batchEntry.caption'
                                            )
                                    }
                                    type={''}
                                />
                            }
                        </button>}
                    </div>
                    {supportQuickInput && (isBatchEntry || fullScreen) &&
                        allowCreateNew && (
                        <TableQuickInput
                            closeBatchEntry={handleBatchEntryToggle}
                            docType={docType}
                            docId={docId}
                            tabId={tabId}
                        />
                    )}
                </div>

                {<button
                    className="btn-icon btn-meta-outline-secondary pointer"
                    onClick={() => toggleFullScreen(!fullScreen)}
                    onMouseEnter={() =>
                        this.toggleTooltip(keymap.TABLE_CONTEXT.TOGGLE_EXPAND)
                    }
                    onMouseLeave={this.toggleTooltip}
                    tabIndex="-1"
                >
                    {fullScreen ? <i className="meta-icon-collapse"/> :
                        <i className="meta-icon-fullscreen"/>}

                    {isTooltipShow === keymap.TABLE_CONTEXT.TOGGLE_EXPAND &&
                        <Tooltips
                            name={keymap.TABLE_CONTEXT.TOGGLE_EXPAND}
                            action={fullScreen ?
                                counterpart.translate(
                                    'window.table.collapse') :
                                counterpart.translate(
                                    'window.table.expand')
                            }
                            type={''}
                        />
                    }
                </button>}
            </div>
        )
    }
}

TableFilter.propTypes = {
    dispatch: PropTypes.func.isRequired
}

TableFilter = connect()(TableFilter);

export default TableFilter;

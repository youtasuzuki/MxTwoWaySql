/*global logger*/
/*
    Handsontable
    ========================

    @file      : Handsontable.js
    @version   : 1.0
    @author    : Buildsystem
    @date      : Fri, 03 Mar 2017 02:39:45 GMT
    @copyright : Copyright All Rights Reserved. 2017
    @license   : Apache 2 / MIT

    Documentation
    ========================
    Describe your widget here.
*/

// Required module list. Remove unnecessary modules, you can always get them back from the boilerplate.
define([
    "dojo/_base/declare",
    "mxui/widget/_WidgetBase",
    "dijit/_TemplatedMixin",

    "mxui/dom",
    "dojo/dom",
    "dojo/dom-prop",
    "dojo/dom-geometry",
    "dojo/dom-class",
    "dojo/dom-style",
    "dojo/dom-construct",
    "dojo/_base/array",
    "dojo/_base/lang",
    "dojo/text",
    "dojo/html",
    "dojo/_base/event",

    "Handsontable/lib/jquery-1.11.2",
    "Handsontable/lib/handsontable.full",
    
    "dojo/text!Handsontable/widget/template/Handsontable.html"
], function (declare, _WidgetBase, _TemplatedMixin, dom, dojoDom, dojoProp, dojoGeometry, dojoClass, dojoStyle, dojoConstruct, dojoArray, lang, dojoText, dojoHtml, dojoEvent, _jQuery,Handsontable, widgetTemplate) {
    "use strict";

    var $ = _jQuery.noConflict(true);
    var table;
    var data;
    
    // Declare widget's prototype.
    return declare("Handsontable.widget.Handsontable", [ _WidgetBase, _TemplatedMixin ], {
        // _TemplatedMixin will create our dom node using this HTML template.;
        templateString: widgetTemplate,

        // Parameters configured in the Modeler.
        resultData: "",
        resultHeader: "",
        resultCount: "",        
        pageNo: "",                
        tableOption:"",
        displayCount:"",

        // Internal variables. Non-primitives created in the prototype are shared between all widget instances.
        _handles: null,
        _contextObj: null,
        _alertDiv: null,
        _readOnly: false,

        // dojo.declare.constructor is called to construct the widget instance. Implement to initialize non-primitive properties.
        constructor: function () {
            logger.debug(this.id + ".constructor");
            this._handles = [];
        },

        // dijit._WidgetBase.postCreate is called after constructing the widget. Implement to do extra setup work.
        postCreate: function () {
            logger.debug(this.id + ".postCreate");

            if (this.readOnly || this.get("disabled") || this.readonly) {
              this._readOnly = true;
            }

            this._updateRendering();
            this._setupEvents();
        },

        // mxui.widget._WidgetBase.update is called when context is changed or initialized. Implement to re-render and / or fetch data.
        update: function (obj, callback) {
            logger.debug(this.id + ".update");

            this._contextObj = obj;
            this._resetSubscriptions();
            this._updateRendering(callback); // We're passing the callback to updateRendering to be called after DOM-manipulation
        },

        // mxui.widget._WidgetBase.enable is called when the widget should enable editing. Implement to enable editing if widget is input widget.
        enable: function () {
          logger.debug(this.id + ".enable");
        },

        // mxui.widget._WidgetBase.enable is called when the widget should disable editing. Implement to disable editing if widget is input widget.
        disable: function () {
          logger.debug(this.id + ".disable");
        },

        // mxui.widget._WidgetBase.resize is called when the page's layout is recalculated. Implement to do sizing calculations. Prefer using CSS instead.
        resize: function (box) {
          logger.debug(this.id + ".resize");
        },

        // mxui.widget._WidgetBase.uninitialize is called when the widget is destroyed. Implement to do special tear-down work.
        uninitialize: function () {
          logger.debug(this.id + ".uninitialize");
            // Clean up listeners, helper objects, etc. There is no need to remove listeners added with this.connect / this.subscribe / this.own.
        },

        // We want to stop events on a mobile device
        _stopBubblingEventOnMobile: function (e) {
            logger.debug(this.id + "._stopBubblingEventOnMobile");
            if (typeof document.ontouchstart !== "undefined") {
                dojoEvent.stop(e);
            }
        },

        // Attach events to HTML dom elements
        _setupEvents: function () {
            logger.debug(this.id + "._setupEvents");
        },

        _execMf: function (mf, guid, cb) {
            logger.debug(this.id + "._execMf");
            if (mf && guid) {
                mx.ui.action(mf, {
                    params: {
                        applyto: "selection",
                        guids: [guid]
                    },
                    callback: lang.hitch(this, function (objs) {
                        if (cb && typeof cb === "function") {
                            cb(objs);
                        }
                    }),
                    error: function (error) {
                        console.debug(error.description);
                    }
                }, this);
            }
        },

        // Rerender the interface.
        _updateRendering: function (callback) {
            logger.debug(this.id + "._updateRendering");

            if (this._contextObj !== null) {
                dojoStyle.set(this.domNode, "display", "block");                
                this._setData();
            } else {
                dojoStyle.set(this.domNode, "display", "none");
            }

            // Important to clear all validations!
            this._clearValidations();

            // The callback, coming from update, needs to be executed, to let the page know it finished rendering
            this._executeCallback(callback, "_updateRendering");
        },

        // Handle validations.
        _handleValidation: function (validations) {
            logger.debug(this.id + "._handleValidation");
            this._clearValidations();
        },

        // Clear validations.
        _clearValidations: function () {
            logger.debug(this.id + "._clearValidations");
            dojoConstruct.destroy(this._alertDiv);
            this._alertDiv = null;
        },

        // Show an error message.
        _showError: function (message) {
            logger.debug(this.id + "._showError");
            if (this._alertDiv !== null) {
                dojoHtml.set(this._alertDiv, message);
                return true;
            }
            this._alertDiv = dojoConstruct.create("div", {
                "class": "alert alert-danger",
                "innerHTML": message
            });
            dojoConstruct.place(this._alertDiv, this.domNode);
        },

        // Add a validation.
        _addValidation: function (message) {
            logger.debug(this.id + "._addValidation");
            this._showError(message);
        },

        // Reset subscriptions.
        _resetSubscriptions: function () {
            logger.debug(this.id + "._resetSubscriptions");
            // Release handles on previous object, if any.
            this.unsubscribeAll();

            // When a mendix object exists create subscribtions.
            if (this._contextObj) {
                this.subscribe({
                    guid: this._contextObj.getGuid(),
                    callback: lang.hitch(this, function (guid) {
                        this._updateRendering();
                    })
                });

                this.subscribe({
                    guid: this._contextObj.getGuid(),
                    attr: this.resultData,
                    callback: lang.hitch(this, function (guid, attr, attrValue) {
                        this._updateRendering();
                    })
                });

                this.subscribe({
                    guid: this._contextObj.getGuid(),
                    val: true,
                    callback: lang.hitch(this, this._handleValidation)
                });
            }
        },

        _executeCallback: function (cb, from) {
            logger.debug(this.id + "._executeCallback" + (from ? " from " + from : ""));
            if (cb && typeof cb === "function") {
                cb();
            }
        },
        
        _setData:function(){
            //現在ページ番号取得
            var nowPageNo = this._contextObj.get(this.pageNo);
            //検索ボタン押下時は0が設定されているので1を設定
            if(nowPageNo==0){
                nowPageNo = 1;
            }
            
            //テーブル情報初期化
            if(table!=null){
                table.destroy();
            }
            table=null;
            _jQuery('#handsonPagingId').empty();
            this._contextObj.set(this.pageNo,0);

            var result = this._contextObj.get(this.resultData);
            //データがある場合はテーブル表示
            if(result!=''){
                var hot  = JSON.parse(this.tableOption)
                hot.data = JSON.parse(result);
                //ページング設定
                this._initPaging(hot.data,nowPageNo);
                hot.colHeaders = JSON.parse(this._contextObj.get(this.resultHeader));
                var obj = document.getElementById('handsonTableId');
                table = Handsontable(obj, hot);
                var con = this._contextObj;
                var pageNoAttribute = this.pageNo;
                _jQuery('#handsonPagingId a').on('click', function () {
                    con.set(pageNoAttribute,_jQuery(this).text());
                    //検索ボタンを押下(検索ボタンのclassに'pagingClick'を設定してあることが前提)
                    _jQuery('.pagingClick').click();
                });
            }
        },
        
        _initPaging:function(data,nowPageNo){
            var dispCnt = this._contextObj.get(this.displayCount);
            if(data != null && dispCnt > 0){
                var pageCount = Math.ceil(this._contextObj.get(this.resultCount)/dispCnt);
                if(pageCount>1){
                    var ulElem;
                    for(var i=1;i <= pageCount;i++){
                        if(i==1){
                            ulElem = _jQuery('<ul class="pagination"></ul>');
                        }
                        if(nowPageNo==i){
                            ulElem.append("<li><span style='border:none;color:black'>"+i+"</span></li>");                            
                        }else{
                            ulElem.append("<li><a href='#' style='border:none;'>"+i+"</a></li>");                                                        
                        }
                        if(i==pageCount){
                            _jQuery('#handsonPagingId').append(ulElem);    
                        }
                    }                    
                }
            }
        }
    });
});

require(["Handsontable/widget/Handsontable"]);

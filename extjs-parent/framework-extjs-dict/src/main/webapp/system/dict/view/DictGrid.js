/**
 * 字典表格
 * @author majian <br/>
 *         date:2015-7-3
 * @version 1.0.0
 */
Ext.define('kalix.dict.view.DictGrid', {
    extend: 'kalix.view.components.common.BaseGrid',
    requires: [
        'kalix.dict.controller.DictGridController'
    ],
    alias: 'widget.dictGrid',
    xtype: 'dictGridPanel',
    controller: {
        type: 'dictGridController',
        cfgForm: 'kalix.dict.view.DictWindow',
        cfgViewForm: 'kalix.dict.view.DictViewWindow',
        cfgModel: 'kalix.dict.model.DictModel'
    },
    columns: {
        defaults: {flex: 1, renderer: 'addTooltip'},
        items: [
            {
                xtype: 'rownumberer',
                text: "行号",
                width: 50,
                flex: 0,
                align: 'center',
                renderer: this.update
            },
            {text: '编号', dataIndex: 'id', hidden: true},
            {text: '类型', dataIndex: 'type'},
            {text: '标签名', dataIndex: 'label'},
            {text: '创建人', dataIndex: 'createBy', flex: 1},
            {
                text: '创建日期', dataIndex: 'creationDate', flex: 1
            },
            {
                xtype: 'securityGridColumnRUD',
                permissions: ['view', 'edit', 'delete']
            }]
    },
    tbar: {
        xtype: 'securityToolbar',
        verifyItems: [
            {
                text: '添加',
                tooltip: '添加字典',
                xtype: 'button',
                permission: 'add',
                iconCls:'iconfont icon-add',
                handler: 'onAdd'
            }
        ]
    }

});
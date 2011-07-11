Ext.namespace("GeoSolutions");
GeoSolutions.CapabilitiesGrid = Ext.extend(Ext.grid.GridPanel, {

    store: null,

    cm: null,

    /**
     * api: property[mapPanel]
     * A :class:`GeoExt.MapPanel` to which layers can be added via this grid.
     */
    mapPanel : null,

    /** api: property[url]
     * A :class:`String` containing an OWS URL to which the GetCapabilities 
     * request is sent.  Necessary if a store is not passed in as a 
     * configuration option.
     */
    url: null,

    autoExpandColumn: "title",

    /** api: method[initComponent]
     * 
     * Initializes the CapabilitiesGrid. Creates and loads a WMS Capabilities 
     * store from the url property if one is not passed as a configuration 
     * option. 
     */
    initComponent: function(){

        if(!this.store){
            this.store = new GeoExt.data.WMSCapabilitiesStore({
                url: this.url + "?service=wms&request=GetCapabilities"
            });

            this.store.load();
        }

        var expander = new Ext.grid.RowExpander({
            tpl : new Ext.Template(
                '<p><b>Abstract:</b> {abstract}</p>')});

        this.plugins = expander;

        this.cm = new Ext.grid.ColumnModel([
            expander,
            {header: "Name", dataIndex: "name", width: 180, sortable: true},
            {id: "title", header: "Title", dataIndex: "title", sortable: true},
            {header: "Queryable", dataIndex: "queryable"}
        ]);

        GeoSolutions.CapabilitiesGrid.superclass.initComponent.call(this);       
    },

    /** api: method[addLayers]
     * :param: base: a boolean indicating whether or not to make the new layer 
     *     a base layer.
     * 
     * Adds a layer to the :class:`GeoExt.MapPanel` of this instance.
     */    
    addLayers : function(base){

        var sm = this.getSelectionModel();

        //for now just use the first selected record
        //TODO: force single selection (until we allow
        //adding group layers)
        var records = sm.getSelections();
        
        var record, layer;
        for(var i = 0; i < records.length; i++){
            Ext.data.Record.AUTO_ID++;
            record = records[i].copy(Ext.data.Record.AUTO_ID);

            layer = record.get("layer").clone();
            record.set("layer", null); //need to do this because record.set compares String(value) to determine equality (dumb)
            record.set("layer", layer);

            /*
             * TODO: deal with srs and maxExtent
             * At this point, we need to think about SRS if we want the layer to
             * have a maxExtent.  For our app, we are dealing with EPSG:4326
             * only.  This will have to be made more generic for apps that use
             * other srs.
             */

            layer.restrictedExtent = OpenLayers.Bounds.fromArray(record.get("llbbox"));

            if (this.alignToGrid) {
                layer.maxExtent = new OpenLayers.Bounds(-180, -90, 180, 90);
            } else {
                layer.maxExtent = layer.restrictedExtent;
            } 

            record.set("background", base && "background");
            this.mapPanel.layers.insert(
                base ? 0 : this.mapPanel.layers.getCount(),
                record
            );
        }

    }
});


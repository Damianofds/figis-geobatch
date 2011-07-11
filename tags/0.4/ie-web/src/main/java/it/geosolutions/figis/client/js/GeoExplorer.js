/**
 * Copyright (c) 2009 OpenGeo
 */

/**
 * api: (define)
 * module = GeoSolutions
 * extends = Ext.Observable
 */

/** api: constructor
 *  .. class:: GeoSolutions(config)
 *     Create a new GeoSolutions application.
 *
 *     Parameters:
 *     config - {Object} Optional application configuration properties.
 *
 *     Valid config properties:
 *     map - {Object} Map configuration object.
 *     wms - {Object} An object with properties whose values are WMS endpoint URLs
 *     alignToGrid - {boolean} if true, align tile requests to the grid 
 *         enforced by tile caches such as GeoWebCache or Tilecache
 *
 *     Valid map config properties:
 *         layers - {Array} A list of layer configuration objects.
 *         center - {Array} A two item array with center coordinates.
 *         zoom - {Number} An initial zoom level.
 *
 *     Valid layer config properties:
 *     name - {String} Required WMS layer name.
 *     title - {String} Optional title to display for layer.
 *
 */
var GeoSolutions = Ext.extend(Ext.util.Observable, {
    
    /** api: property[map]
     * :class:`OpenLayers.Map` The application's map.
     */
    map: null,
    
    /** private: property[layers]
     * A :class:`GeoExt.data.LayerStore` containing a record for each layer
     * on the map.
     */
    layers: null,

    /**
     * private: property[mapPanel]
     * the :class:`GeoExt.MapPanel` instance for the main viewport
     */
    mapPanel: null,

    /**
     * api: config[alignToGrid]
     * A boolean indicating whether or not to restrict tile request to tiled
     * mapping service recommendation.  
     *
     * True => align to grid 
     * False => unrestrained tile requests
     */
    alignToGrid: false,

    /**
     * private: property[capGrid]
     * :class:`Ext.Window` The window containing the CapabilitiesGrid panel to 
     * use when the user is adding new layers to the map.
     */
    capGrid: null,

    /**
     * private: property[popupCache]
     * :class:`Object` An object containing references to visible popups so that
     * we can insert responses from multiple requests.
     *
     * ..seealso:: :method:`GeoSolutions.displayPopup()`
     */
    popupCache: null,
    
    /** api: property[layerSources]
     * A :class:`Ext.data.Store` containing one 
     * :class:`GeoExt.data.WMSCapabilitiesStore` for each WMS service in use by
     * the application, along with service-specific metadata like the service 
     * name.
     */
    layerSources: null,

    constructor: function(config) {
        this.popupCache = {};

        var query = Ext.urlDecode(document.location.search.substr(1));
        var queryConfig = Ext.util.JSON.decode(query.q);
        
        this.initialConfig = Ext.apply({}, queryConfig, config);
        Ext.apply(this, this.initialConfig);
        
        // add any custom application events
        this.addEvents(
            /**
             * Event: ready
             * Fires when application is ready for user interaction.
             */
            "ready");
        
        // pass on any proxy config to OpenLayers
        if(this.proxy) {
            OpenLayers.ProxyHost = this.proxy;
        }
        
        this.load();
    },

    /**
     * private: method[load]
     * Called at the end of construction.  This initiates the sequence that
     * prepares the application for use, including tasks such as loading 
     * capabilities from remote servers, populating the map, etc.
     */
    load: function() {
        this.layerSources = new Ext.data.SimpleStore({
            fields: ["identifier", "name", "store", "url"],
            data: []
        });
        
        var dispatchQueue = [
            // create layout as soon as Ext says ready
            function(done) {
                Ext.onReady(function() {
                    this.createLayout();
                    done();
                }, this);
            }
        ];
        
        for (var id in this.wms) {
            // Load capabilities for each wms passed through the configuration.
            dispatchQueue.push(
                (function(id) {
                    // Create a new scope for 'id'.
                    return function(done){
                        this.addSource(this.wms[id], id, done, done);
                    }; 
                })(id));
        }
        
        GeoSolutions.util.dispatch(
            dispatchQueue,
            
            // activate app when the above are both done
            this.activate, 
            this);
    },
    
    /** private: method[addSource]
     * Add a new WMS server to GeoSolutions. The id parameter is optional,
     * and will be given a default if not specified; success and fail 
     * are also optional, and scope only applies if success or fail
     * is passed in.
     */
    addSource: function(url, id, success, fail, scope) {
        scope = scope || this;
        success = OpenLayers.Function.bind(success, scope);
        fail = OpenLayers.Function.bind(fail, scope);
        
        id = id || OpenLayers.Util.createUniqueID("source");
        var capsURL = this.createWMSCapabilitiesURL(url);
                        var store = new GeoExt.data.WMSCapabilitiesStore();

        OpenLayers.Request.GET({
            proxy: this.proxy, 
            url: capsURL,
            success: function(request){
                var store = new GeoExt.data.WMSCapabilitiesStore({
                            fields:  [
                                {name: "name", type: "string"},
                                {name: "abstract", type: "string"},
                                {name: "queryable", type: "boolean"},
                                {name: "formats"},
                                {name: "styles"},
                                {name: "llbbox"},
                                {name: "minScale"},
                                {name: "maxScale"},
                                {name: "prefix"},
                                
                                // Added for GeoSolutions.
                                {name: "group", type: "string"},
                                {name: "source_id", type: "string"}
                            ]
                        });
                var xml = request.responseXML;
                var data = (xml && xml.documentElement) ?
                    xml : request.responseText;
                
                try {
                    // Read the response. It's important to note that the
                    // WMSCapabilitiesStore reads the data as well, though
                    // we need to do it ourselves in order to maintain
                    // low coupling.
                    var format = new OpenLayers.Format.WMSCapabilities();
                    var extractedData = format.read(data);
                    
                    store.loadData(data);
                } catch(err) {
                    OpenLayers.Console.error("Could not load source: " + url);
                    fail();
                    return;
                }
                
                // MODERATELY LARGE DIRTY HACK!
                // Tell each layer where it came from.
                store.each(function(record) {
                    record.set("source_id", id);
                }, this);
                
                var record = new this.layerSources.recordType({
                    url: url,
                    store: store,
                    identifier: id,
                    name: extractedData.service.title || id
                });
                
                this.layerSources.add(record);
                success(record);
            },
            failure: function(){
                OpenLayers.Console.error("Couldn't get capabilities document for wms '" + id + "'.");
                fail();
            },
            scope: this
        });
    },
    
    /** private: method[createWMSCapabilitiesURL]
     * Given the URL to an OWS service endpoint, generate a GET request URL for
     * the service's WMS capabilities.
     */
    createWMSCapabilitiesURL: function(url) {
        var args = {
            SERVICE: "WMS",
            REQUEST: "GetCapabilities",
            VERSION: "1.1.1"
        };
        var argIndex = url.indexOf("?");
        if(argIndex > -1) {
            var search = url.substring(url.indexOf("?")+1);
            url = url.replace(search, Ext.urlEncode(Ext.apply(
                Ext.urlDecode(search), args)));
        } else {
            url = url + "?" + Ext.urlEncode(args);
        }

        return url;
    },
    
    /** private: method[createLayout]
     * Create the various parts that compose the layout.
     */
    createLayout: function() {
        
        // create the map
        var mapConfig = this.initialConfig.map || {};
        this.map = new OpenLayers.Map({
            theme: mapConfig.theme || null,
            allOverlays: ("allOverlays" in mapConfig) ? mapConfig.allOverlays : true,
            controls: [new OpenLayers.Control.PanPanel(),
                       new OpenLayers.Control.ZoomPanel()],
            numZoomLevels: mapConfig.numZoomLevels || 20
        });

        //** Remove this code when OpenLayers #2069 is closed **
        var onDoubleClick = function(ctrl, evt) { 
 	        OpenLayers.Event.stop(evt ? evt : window.event); 
        };
        var controls = this.map.controls[0].controls;
        for(var i = 0; i < controls.length; i++){
            OpenLayers.Event.observe(controls[i].panel_div, "dblclick",  
 	                             OpenLayers.Function.bind(onDoubleClick, this.map.controls[0], controls[i])); 
        }        
        //******************************************************

        //TODO: make this more configurable
        this.map.events.on({
            "preaddlayer" : function(evt){
                if(evt.layer.mergeNewParams){
                    var maxExtent = evt.layer.maxExtent;
                    evt.layer.mergeNewParams({
                        transparent: true,
                        format: "image/png",
                        tiled: true,
                        tilesorigin: [maxExtent.left, maxExtent.bottom]
                    });
                }
            },
            scope : this
        });
        

        // place map in panel
        this.mapPanel = new GeoExt.MapPanel({
            layout: "anchor",
            border: true,
            region: "center",
            map: this.map,
            // TODO: update the OpenLayers.Map constructor to accept an initial center
            center: mapConfig.center && new OpenLayers.LonLat(mapConfig.center[0], mapConfig.center[1]),
            // TODO: update the OpenLayers.Map constructor to accept an initial zoom
            zoom: mapConfig.zoom,
            items: [
                {
                    xtype: "gx_zoomslider",
                    vertical: true,
                    height: 100,
                    plugins: new GeoExt.ZoomSliderTip({
                        template: "<div>Zoom Level: {zoom}</div>"
                    })
                },
                this.createMapOverlay()
            ]
        });
        
        // create layer store
        this.layers = this.mapPanel.layers;

        var addLayerButton = new Ext.Button({
            tooltip : "Add Layers",
            disabled: true,
            iconCls: "icon-addlayers",
            handler : this.showCapabilitiesGrid,
            scope: this
        });
        this.on("ready", function() {addLayerButton.enable();});

        var removeLayerAction = new Ext.Action({
            text: "Remove Layer",
            iconCls: "icon-removelayers",
            disabled: true,
            tooltip: "Remove Layer",
            handler: function() {
                var node = layerTree.getSelectionModel().getSelectedNode();
                if(node && node.layer) {
                    var layer = node.layer;
                    var store = node.layerStore;
                    var record = store.getAt(store.findBy(function(record) {
                        return record.get("layer") === layer;
                    }));
                    store.remove(record);
                    removeLayerAction.disable();
                }
            }
        });

        var treeRoot = new Ext.tree.TreeNode({
            text: "Layers",
            expanded: true,
            isTarget: false,
            allowDrop: false
        });
        treeRoot.appendChild(new GeoSolutions.GroupContainer({
            text: "Overlays",
            iconCls: "gx-folder",
            expanded: true,
            layerStore: this.mapPanel.layers,
            singleClickExpand: true,
            allowDrag: false,
            listeners: {
                append: function(tree, node) {
                    node.expand();
                }
            }
        }));
        treeRoot.appendChild(new GeoSolutions.GroupContainer({
            text: "Base Layers",
            iconCls: "gx-folder",
            expanded: true,
            group: "background",
            defaults: {checkedGroup: "background"},
            layerStore: this.mapPanel.layers,
            singleClickExpand: true,
            allowDrag: false,
            listeners: {
                append: function(tree, node) {
                    node.expand();
                }
            }
        }));
        
        var layerTree = new Ext.tree.TreePanel({
            root: treeRoot,
            rootVisible: false,
            border: false,
            enableDD: true,
            selModel: new Ext.tree.DefaultSelectionModel({
                listeners: {
                    beforeselect: function(sel, node) {
                        if(node && node.layer) {
                            // allow removal if more than one non-vector layer
                            var count = this.mapPanel.layers.queryBy(function(r) {
                                return !(r.get("layer") instanceof OpenLayers.Layer.Vector);
                            }).getCount();
                            if(count > 1) {
                                removeLayerAction.enable();
                            }
                        }
                    },
                    scope: this
                }
            }),
            listeners: {
                contextmenu: function(node, e) {
                    if(node && node.layer) {
                        node.select();
                        var c = node.getOwnerTree().contextMenu;
                        c.contextNode = node;
                        c.showAt(e.getXY());
                    }
                },
                // TODO: remove this when http://www.geoext.org/trac/geoext/ticket/112 is closed
                startdrag: function(tree, node, evt) {
                    node.getUI().checkbox.checked = node.attributes.checked;
                },                
                scope: this
            },
            contextMenu: new Ext.menu.Menu({
                items: [
                    {
                        text: "Zoom to Layer Extent",
                        iconCls: "icon-zoom-visible",
                        handler: function() {
                            var node = layerTree.getSelectionModel().getSelectedNode();
                            if(node && node.layer) {
                                this.map.zoomToExtent(node.layer.restrictedExtent);
                            }
                        },
                        scope: this
                    },
                    removeLayerAction
                ]
            })
        });

        var layersContainer = new Ext.Panel({
            autoScroll: true,
            border: false,
            region: 'center',
            title: "Layers",
            items: [layerTree],
            tbar: [
                addLayerButton,
                Ext.apply(new Ext.Button(removeLayerAction), {text: ""})
            ]
        });

        var legendContainer = new GeoExt.LegendPanel({
            title: "Legend",
            border: false,
            region: 'south',
            height: 200,
            collapsible: true,
            split: true,
            autoScroll: true,
            ascending: false,
            map: this.map,
            defaults: {cls: 'legend-item'}
        });

        var westPanel = new Ext.Panel({
            border: true,
            layout: "border",
            region: "west",
            width: 250,
            split: true,
            collapsible: true,
            collapseMode: "mini",
            items: [
                layersContainer, legendContainer
            ]
        });
        
        var toolbar = new Ext.Toolbar({
            xtype: "toolbar",
            region: "north",
            disabled: true,
            items: this.createTools()
        });
        this.on("ready", function() {
            // enable only those items that were not specifically disabled
            var disabled = toolbar.items.filterBy(function(item) {
                return item.initialConfig && item.initialConfig.disabled;
            });
            toolbar.enable();
            disabled.each(function(item) {
                item.disable();
            });
        });
        
        var viewport = new Ext.Viewport({
            layout: "fit",
            hideBorders: true,
            items: {
                layout: "border",
                deferredRender: false,
                items: [
                    toolbar,
                    this.mapPanel,
                    westPanel
                ]
            }
        });    
    },
    
    /** private: method[activate]
     * Activate the application.  Call after application is configured.
     */
    activate: function() {
        
        // add any layers from config
        this.addLayers();

        // initialize tooltips
        Ext.QuickTips.init();
        
        this.fireEvent("ready");

    },
    
    /** private: method[addLayers]
     * Construct the layer store to be used with the map (referenced as 
     * :attr:`GeoSolutions.layers`).
     */
    addLayers: function() {
        var mapConfig = this.initialConfig.map;

        if(mapConfig && mapConfig.layers) {
            var records = [];
            
            for(var i = 0; i < mapConfig.layers.length; ++i) {
                var conf = mapConfig.layers[i];
                var index = this.layerSources.findBy(function(r) {
                    return r.get("identifier") === conf.wms;
                });
                
                if (index == -1) {
                    continue;
                }
                
                var storeRecord = this.layerSources.getAt(index);
                var store = storeRecord.data.store;

                var id = store.findBy(function(r) {
                    return r.get("name") === conf.name;
                });
                
                var record;
                var base;
                if (id >= 0) {
                    /**
                     * If the same layer is added twice, it will get replaced
                     * unless we give each record a unique id.  In addition, we
                     * need to clone the layer so that the map doesn't assume
                     * the layer has already been added.  Finally, we can't
                     * simply set the record layer to the cloned layer because
                     * record.set compares String(value) to determine equality.
                     * 
                     * TODO: suggest record.clone
                     */
                    Ext.data.Record.AUTO_ID++;
                    record = store.getAt(id).copy(Ext.data.Record.AUTO_ID);
                    layer = record.get("layer").clone();
                    record.set("layer", null);
                    record.set("layer", layer);
                    
                    // set layer max extent from capabilities
                    // TODO: make this SRS independent
                    layer.restrictedExtent = OpenLayers.Bounds.fromArray(record.get("llbbox"));
                    
                    if (this.alignToGrid) {
                        layer.maxExtent = new OpenLayers.Bounds(-180, -90, 180, 90);
                    } else {
                        layer.maxExtent = layer.restrictedExtent;
                    }


                    // set layer visibility from config
                    layer.visibility = ("visibility" in conf) ? conf.visibility : true;
                    
                    // set layer title from config
                    if (conf.title) {
                        /**
                         * Because the layer title data is duplicated, we have
                         * to set it in both places.  After records have been
                         * added to the store, the store handles this
                         * synchronization.
                         */
                        layer.setName(conf.title);
                        record.set("title", conf.title);
                    }

                    record.set("group", conf.group);
                    
                    // set any other layer configuration
                    // ensures that background layers are on the bottom
                    if(record.get("group") === "background") {
                        records.unshift(record);
                    } else {
                        records.push(record);
                    }
                }
                
            }
            
            this.layers.add(records);

            // set map center
            if(this.mapPanel.center) {
                // zoom does not have to be defined
                this.map.setCenter(this.mapPanel.center, this.mapPanel.zoom);
            } else if (this.mapPanel.extent) {
                this.map.zoomToExtent(this.mapPanel.extent);
            } else {
                this.map.zoomToMaxExtent();
            }
            
        }
    },

    /**
     * private: method[initCapGrid]
     * Constructs a window with a capabilities grid.
     */
    initCapGrid: function(){

        // TODO: Might be nice to subclass some of these things into
        // into their own classes.

        var firstSource = this.layerSources.getAt(0);

        var capGridPanel = new GeoSolutions.CapabilitiesGrid({
            store: firstSource.data.store,
            mapPanel : this.mapPanel,
            layout: 'fit',
            region: 'center',
            autoScroll: true,
            alignToGrid: this.alignToGrid,
            listeners: {
                rowdblclick: function(panel, index, evt) {
                    panel.addLayers();
                }
            }
        });

        var sourceComboBox = new Ext.form.ComboBox({
            store: this.layerSources,
            valueField: "identifier",
            displayField: "name",
            triggerAction: "all",
            editable: false,
            allowBlank: false,
            forceSelection: true,
            mode: "local",
            value: firstSource.data.identifier,
            listeners: {
                select: function(combo, record, index) {
                    capGridPanel.reconfigure(record.data.store, capGridPanel.getColumnModel());
                },
                scope: this
            }
        });

        var capGridToolbar = null;

        if (this.proxy || this.layerSources.getCount() > 1) {
            capGridToolbar = [
                new Ext.Toolbar.TextItem({
                    text: "View available data from:"
                }),
                sourceComboBox
            ];
        }

        if (this.proxy) {
            capGridToolbar.push(new Ext.Button({
                text: "or add a new server.",
                handler: function() {
                    newSourceWindow.show();
                }
            }));
        }

        var newSourceWindow = new GeoSolutions.NewSourceWindow({modal: true});
        
        newSourceWindow.on("server-added", function(url) {
            newSourceWindow.setLoading();
            
            var success = function(record) {
                // The combo box will automatically update when a new item
                // is added to the layerSources store. Now all we have to
                // do is select it. Note: There's probably a better way to do this, 
                // but there doesn't seem to be another way to get the select event
                // to fire.
                var index = this.layerSources.find("identifier", record.get("identifier"));
                sourceComboBox.onSelect(record, index);
                
                // Close the new source window.
                newSourceWindow.hide();
            };
            
            var failure = function() {
                newSourceWindow.setError("Error contacting server.\nPlease check the url and try again.");
            };
            
            this.addSource(url, null, success, failure, this);
        }, this);
        
        this.capGrid = new Ext.Window({
            title: "Available Layers",
            closeAction: 'hide',
            layout: 'border',
            height: 300,
            width: 600,
            modal: true,
            items: [
                capGridPanel
            ],
            tbar: capGridToolbar,
            bbar: [
                "->",
                new Ext.Button({
                    text: "Add Layers",
                    iconCls: "icon-addlayers",
                    handler: function(){
                        capGridPanel.addLayers();
                    },
                    scope : this
                }),
                new Ext.Button({
                    text: "Done",
                    handler: function() {
                        this.capGrid.hide();
                    },
                    scope: this
                })
            ],
            listeners: {
                hide: function(win){
                    capGridPanel.getSelectionModel().clearSelections();
                }
            }
        });
 
    },

    /** private: method[showCapabilitiesGrid]
     * Shows the window with a capabilities grid.
     */
    showCapabilitiesGrid: function() {
        if(!this.capGrid) {
            this.initCapGrid();
        }
        this.capGrid.show();
    },

    /** private: method[createMapOverlay]
     * Builds the :class:`Ext.Panel` containing components to be overlaid on the
     * map, setting up the special configuration for its layout and 
     * map-friendliness.
     */
    createMapOverlay: function() {
        var scaleLinePanel = new Ext.Panel({
            cls: 'olControlScaleLine overlay-element overlay-scaleline',
            border: false
        });

        scaleLinePanel.on('render', function(){
            var scaleLine = new OpenLayers.Control.ScaleLine({
                div: scaleLinePanel.body.dom
            });

            this.map.addControl(scaleLine);
            scaleLine.activate();
        }, this);

        var zoomStore = new GeoExt.data.ScaleStore({
            map: this.map
        });

        var zoomSelector = new Ext.form.ComboBox({
            emptyText: 'Zoom level',
            tpl: '<tpl for="."><div class="x-combo-list-item">1 : {[parseInt(values.scale)]}</div></tpl>',
            editable: false,
            triggerAction: 'all',
            mode: 'local',
            store: zoomStore,
            width: 110
        });

        zoomSelector.on('click', function(evt){evt.stopEvent();});
        zoomSelector.on('mousedown', function(evt){evt.stopEvent();});

        zoomSelector.on('select', function(combo, record, index) {
                this.map.zoomTo(record.data.level);
            },
            this);

        var zoomSelectorWrapper = new Ext.Panel({
            items: [zoomSelector],
            cls: 'overlay-element overlay-scalechooser',
            border: false });

        this.map.events.register('zoomend', this, function() {
            var scale = zoomStore.queryBy(function(record){
                return this.map.getZoom() == record.data.level;
            });

            if (scale.length > 0) {
                scale = scale.items[0];
                zoomSelector.setValue("1 : " + parseInt(scale.data.scale, 10));
            } else {
                if (!zoomSelector.rendered) {
                    return;
                }
                zoomSelector.clearValue();
            }
        });

        var mapOverlay = new Ext.Panel({
            // title: "Overlay",
            cls: 'map-overlay',
            items: [
                scaleLinePanel,
                zoomSelectorWrapper
            ]
        });


        mapOverlay.on("afterlayout", function(){
            scaleLinePanel.body.dom.style.position = 'relative';
            scaleLinePanel.body.dom.style.display = 'inline';

            mapOverlay.getEl().on("click", function(x){x.stopEvent();});
            mapOverlay.getEl().on("mousedown", function(x){x.stopEvent();});
        }, this);

        return mapOverlay;
    },

    /** private: method[createTools]
     * Create the toolbar configuration for the main panel.  This method can be 
     * overridden in derived explorer classes such as :class:`GeoSolutions.Full`
     * or :class:`GeoSolutions.Embed` to provide specialized controls.
     */
    createTools: function() {

        var toolGroup = "toolGroup";

        // create a navigation control
        var navAction = new GeoExt.Action({
            tooltip: "Pan Map",
            iconCls: "icon-pan",
            enableToggle: true,
            pressed: true,
            allowDepress: false,
            control: new OpenLayers.Control.Navigation(),
            map: this.map,
            toggleGroup: toolGroup
        });

        // create a navigation history control
        var historyControl = new OpenLayers.Control.NavigationHistory();
        this.map.addControl(historyControl);

        // create actions for previous and next
        var navPreviousAction = new GeoExt.Action({
            tooltip: "Zoom to Previous Extent",
            iconCls: "icon-zoom-previous",
            disabled: true,
            control: historyControl.previous
        });
        
        var navNextAction = new GeoExt.Action({
            tooltip: "Zoom to Next Extent",
            iconCls: "icon-zoom-next",
            disabled: true,
            control: historyControl.next
        });

        // create a get feature info control
        var info = {controls: []};
        var infoButton = new Ext.Button({
            tooltip: "Get Feature Info",
            iconCls: "icon-getfeatureinfo",
            toggleGroup: toolGroup,
            enableToggle: true,
            allowDepress: false,
            toggleHandler: function(button, pressed) {
                for (var i = 0, len = info.controls.length; i < len; i++){
                    if(pressed) {
                        info.controls[i].activate();
                    } else {
                        info.controls[i].deactivate();
                    }
                }
            }
        });

        var updateInfo = function() {
            var queryableLayers = this.mapPanel.layers.queryBy(function(x){
                return x.get("queryable");
            });

            var map = this.mapPanel.map;
            var control;
            for (var i = 0, len = info.controls.length; i < len; i++){
                control = info.controls[i];
                control.deactivate();  // TODO: remove when http://trac.openlayers.org/ticket/2130 is closed
                control.destroy();
            }

            info.controls = [];
            queryableLayers.each(function(x){
                var control = new OpenLayers.Control.WMSGetFeatureInfo({
                    url: x.get("layer").url,
                    queryVisible: true,
                    layers: [x.get("layer")],
                    eventListeners: {
                        getfeatureinfo: function(evt) {
                            this.displayPopup(evt, x.get("title") || x.get("name"));
                        },
                        scope: this
                    }
                });
                map.addControl(control);
                info.controls.push(control);
                if(infoButton.pressed) {
                    control.activate();
                }
            }, this);
        };

        this.mapPanel.layers.on("update", updateInfo, this);
        this.mapPanel.layers.on("add", updateInfo, this);
        this.mapPanel.layers.on("remove", updateInfo, this);

        // create split button for measure controls
        var activeIndex = 0;
        var measureSplit = new Ext.SplitButton({
            iconCls: "icon-measure-length",
            tooltip: "Measure",
            enableToggle: true,
            toggleGroup: toolGroup, // Ext doesn't respect this, registered with ButtonToggleMgr below
            allowDepress: false, // Ext doesn't respect this, handler deals with it
            handler: function(button, event) {
                // allowDepress should deal with this first condition
                if(!button.pressed) {
                    button.toggle();
                } else {
                    button.menu.items.itemAt(activeIndex).setChecked(true);
                }
            },
            listeners: {
                toggle: function(button, pressed) {
                    // toggleGroup should handle this
                    if(!pressed) {
                        button.menu.items.each(function(i) {
                            i.setChecked(false);
                        });
                    }
                },
                render: function(button) {
                    // toggleGroup should handle this
                    Ext.ButtonToggleMgr.register(button);
                }
            },
            menu: new Ext.menu.Menu({
                items: [
                    new Ext.menu.CheckItem(
                        new GeoExt.Action({
                            text: "Length",
                            iconCls: "icon-measure-length",
                            toggleGroup: toolGroup,
                            group: toolGroup,
                            allowDepress: false,
                            map: this.map,
                            control: this.createMeasureControl(
                                OpenLayers.Handler.Path, "Length"
                            )
                        })
                    ),
                    new Ext.menu.CheckItem(
                        new GeoExt.Action({
                            text: "Area",
                            iconCls: "icon-measure-area",
                            toggleGroup: toolGroup,
                            group: toolGroup,
                            allowDepress: false,
                            map: this.map,
                            control: this.createMeasureControl(
                                OpenLayers.Handler.Polygon, "Area"
                            )
                        })
                    )
                ]
            })
        });
        measureSplit.menu.items.each(function(item, index) {
            item.on({checkchange: function(item, checked) {
                measureSplit.toggle(checked);
                if(checked) {
                    activeIndex = index;
                    measureSplit.setIconClass(item.iconCls);
                }
            }});
        });
    
        var tools = [
            navAction,
            infoButton,
            measureSplit,
            "-",
            new Ext.Button({
                handler: function(){
                    this.map.zoomIn();
                },
                tooltip: "Zoom In",
                iconCls: "icon-zoom-in",
                scope: this
            }),
            new Ext.Button({
                tooltip: "Zoom Out",
                handler: function(){
                    this.map.zoomOut();
                },
                iconCls: "icon-zoom-out",
                scope: this
            }),
            navPreviousAction,
            navNextAction,
            new Ext.Button({
                tooltip: "Zoom to Visible Extent",
                iconCls: "icon-zoom-visible",
                handler: function() {
                    var extent, layer;
                    for(var i=0, len=this.map.layers.length; i<len; ++i) {
                        layer = this.map.layers[i];
                        if(layer.getVisibility()) {
                            if(extent) {
                                extent.extend(layer.maxExtent);
                            } else {
                                extent = layer.maxExtent.clone();
                            }
                        }
                    }
                    if(extent) {
                        this.map.zoomToExtent(extent);
                    }
                },
                scope: this
            })
        ];

        return tools;
    },

    /** private: method[createMeasureControl]
     * :param: handlerType: the :class:`OpenLayers.Handler` for the measurement
     *     operation
     * :param: title: the string label to display alongside results
     *
     * Convenience method for creating a :class:`OpenLayers.Control.Measure` 
     * control
     */
    createMeasureControl: function(handlerType, title) {
        
        var styleMap = new OpenLayers.StyleMap({
            "default": new OpenLayers.Style(null, {
                rules: [new OpenLayers.Rule({
                    symbolizer: {
                        "Point": {
                            pointRadius: 4,
                            graphicName: "square",
                            fillColor: "white",
                            fillOpacity: 1,
                            strokeWidth: 1,
                            strokeOpacity: 1,
                            strokeColor: "#333333"
                        },
                        "Line": {
                            strokeWidth: 3,
                            strokeOpacity: 1,
                            strokeColor: "#666666",
                            strokeDashstyle: "dash"
                        },
                        "Polygon": {
                            strokeWidth: 2,
                            strokeOpacity: 1,
                            strokeColor: "#666666",
                            fillColor: "white",
                            fillOpacity: 0.3
                        }
                    }
                })]
            })
        });

        var cleanup = function() {
            if (measureToolTip) {
                measureToolTip.destroy();
            }   
        };

        var makeString = function(metricData) {
            var metric = metricData.measure;
            var metricUnit = metricData.units;
            
            measureControl.displaySystem = "english";
            
            var englishData = metricData.geometry.CLASS_NAME.indexOf("LineString") > -1 ?
            measureControl.getBestLength(metricData.geometry) :
            measureControl.getBestArea(metricData.geometry);

            var english = englishData[0];
            var englishUnit = englishData[1];
            
            measureControl.displaySystem = "metric";
            var dim = metricData.order == 2 ? 
            '<sup>2</sup>' :
            '';
            
            return metric.toFixed(2) + " " + metricUnit + dim + "<br>" + 
                english.toFixed(2) + " " + englishUnit + dim;
        };
        
        var measureToolTip; 
        var measureControl = new OpenLayers.Control.Measure(handlerType, {
            persist: true,
            handlerOptions: {layerOptions: {styleMap: styleMap}},
            eventListeners: {
                measurepartial: function(event) {
                    cleanup();
                    measureToolTip = new Ext.ToolTip({
                        html: makeString(event),
                        title: title,
                        autoHide: false,
                        closable: true,
                        draggable: false,
                        mouseOffset: [0, 0],
                        showDelay: 1,
                        listeners: {hide: cleanup}
                    });
                    if(event.measure > 0) {
                        var px = measureControl.handler.lastUp;
                        var p0 = this.mapPanel.getPosition();
                        measureToolTip.targetXY = [p0[0] + px.x, p0[1] + px.y];
                        measureToolTip.show();
                    }
                },
                measure: function(event) {
                    cleanup();                    
                    measureToolTip = new Ext.ToolTip({
                        target: Ext.getBody(),
                        html: makeString(event),
                        title: title,
                        autoHide: false,
                        closable: true,
                        draggable: false,
                        mouseOffset: [0, 0],
                        showDelay: 1,
                        listeners: {
                            hide: function() {
                                measureControl.cancel();
                                cleanup();
                            }
                        }
                    });
                },
                deactivate: cleanup,
                scope: this
            }
        });

        return measureControl;
    },

    /** private: method[displayPopup]
     * :param: evt: the event object from a 
     *     :class:`OpenLayers.Control.GetFeatureInfo` control
     * :param: title: a String to use for the title of the results section 
     *     reporting the info to the user
     */
    displayPopup: function(evt, title) {
        var popup;
        var popupKey = evt.xy.x + "." + evt.xy.y;

        if (!(popupKey in this.popupCache)) {
            var lonlat = this.map.getLonLatFromPixel(evt.xy);
            popup = new GeoExt.Popup({
                title: "Feature Info",
                layout: "accordion",
                lonlat: lonlat,
                map: this.mapPanel,
                width: 250,
                height: 300,
                listeners: {
                    close: (function(key) {
                        return function(panel){
                            delete this.popupCache[key];
                        };
                    })(popupKey),
                    scope: this
                }
            });
            popup.show();
            this.popupCache[popupKey] = popup;
        } else {
            popup = this.popupCache[popupKey];
        }

        var html = evt.text;
        if (!(html === '' || html.match(/<body>\s*<\/body>/))) {
            popup.add({
                title: title,
                layout: "fit",
                html: html,
                autoScroll: true,
                autoWidth: true,
                collapsible: true
            });
        }

        popup.doLayout();
    },


    /**
     * private: method[bookmark]
     * :return: the URL :class:`String` that was displayed to the user
     *
     * Creates a window that shows the user a URL that can be used to
     * reload the map in its current configuration.
     */ 
    bookmark: function(){

        var params = Ext.apply(
            OpenLayers.Util.getParameters(),
            {q: Ext.util.JSON.encode(this.extractConfiguration())}
        );
        
        // disregard any hash in the url, but maintain all other components
        var url =
            document.location.href.split("?").shift() +
            "?" + Ext.urlEncode(params);

        var win = new Ext.Window({
            title: "Bookmark URL",
            layout: 'form',
            labelAlign: 'top',
            modal: true,
            bodyStyle: "padding: 5px",
            width: 300,
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Permalink',
                readOnly: true,
                anchor: "100%",
                selectOnFocus: true,
                value: url
            }]
        });

        win.show();
        win.items.first().selectText();

        return url;
    },
    
    /**
     * private: method[extractConfiguration]
     * :return: an :class:`Object` representing the app's current configuration.
     */ 
    extractConfiguration: function() {

        var center = this.map.getCenter();        
        var config = {
            wms: {},
            map: {
                center: [center.lon, center.lat],
                zoom: this.map.zoom,
                layers: []
            }
        };
        
        this.layers.each(function(layerRecord){
            var layer = layerRecord.get('layer');
            if (layer.displayInLayerSwitcher) {
                
                // Get the source of this layer.
                var index = this.layerSources.find("identifier", layerRecord.get("source_id"));
                var source = this.layerSources.getAt(index);
                
                if (source === null) {
                    OpenLayers.Console.error("Could not find source for layer '" + layerRecord.get("name") + "'");
                    
                    // Return; error gracefully. (This is debatable.)
                    return;
                }
                // add source
                config.wms[source.get("identifier")] = source.get("url");
                
                config.map.layers.push({
                    name: layerRecord.get("name"),
                    title: layerRecord.get("title"),
                    visibility: layer.getVisibility(),
                    group: layerRecord.get("group"),
                    wms: source.get("identifier")
                });
            }
        }, this);
        
        return config;
    },

    /** private: method[displayAppInfo]
     * Display an informational dialog about the application.
     */
    displayAppInfo: function() {
        var appInfo = new Ext.Panel({
            title: "GeoSolutions",
            html: "<iframe style='border: none; height: 100%; width: 100%' src='about.html'><a target='_blank' href='about.html'>About GeoSolutions</a> </iframe>"
        });

        var about = Ext.applyIf(this.about, {
            title: '', 
            "abstract": '', 
            contact: ''
        });

        var mapInfo = new Ext.Panel({
            title: "Map Info",
            html: '<div class="gx-info-panel">' +
                  '<h2>Title</h2><p>' + about.title +
                  '</p><h2>Description</h2><p>' + about['abstract'] +
                  '</p> <h2>Contact</h2><p>' + about.contact +'</p></div>',
            height: 'auto',
            width: 'auto'
        });

        var tabs = new Ext.TabPanel({
            activeTab: 0,
            items: [mapInfo, appInfo]
        });

        var win = new Ext.Window({
            title: "About this Map",
            modal: true,
            layout: "fit",
            width: 300,
            height: 300,
            items: [tabs]
        });
        win.show();
    }
});




package com.atakmap.android.apolloedge;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.atakmap.android.apolloedge.plugin.R;
import com.atakmap.android.hierarchy.HierarchyListFilter;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.android.hierarchy.action.Search;
import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.hierarchy.action.Visibility2;
import com.atakmap.android.hierarchy.items.AbstractHierarchyListItem2;
import com.atakmap.android.hierarchy.items.MapItemUser;
import com.atakmap.android.maps.DeepMapItemQuery;
import com.atakmap.android.maps.DefaultMapGroup;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MapView.RenderStack;
import com.atakmap.android.maps.MetaShape;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.android.menu.MenuLayoutWidget;
import com.atakmap.android.overlay.AbstractMapOverlay2;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.Layer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Example map overlay that is displayed within Overlay Manager
 */
public class ApolloEdgeMapOverlay extends AbstractMapOverlay2 {

    private static final String TAG = "ApolloEdgeMapOverlay";

    private final MapView _mapView;
    private final Context _plugin;
    private final DefaultMapGroup _group;

    public ApolloEdgeMapOverlay(MapView mapView, Context plugin) {
        _mapView = mapView;
        _plugin = plugin;
        _group = new DefaultMapGroup("Apollo Map Group");
        _group.setMetaBoolean("addToObjList", false);
    }

    @Override
    public String getIdentifier() {
        return TAG;
    }

    @Override
    public String getName() {
        return "Apollo Edge"; 
    }

    @Override
    public MapGroup getRootGroup() {
        return _group;
    }

    @Override
    public DeepMapItemQuery getQueryFunction() {
        return null;
    }

    @Override
    public HierarchyListItem getListModel(BaseAdapter adapter,
                                          long capabilities,
                                          HierarchyListFilter prefFilter) {
        return null;
    }
}

package sk.vava.mhd

import android.content.Context
import android.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory


class CustomIconGenerator(val context: Context) : net.sharewire.googlemapsclustering.IconGenerator<MyItem> {
    override fun getClusterIcon(cluster: net.sharewire.googlemapsclustering.Cluster<MyItem>): BitmapDescriptor {
        val marker2 = com.google.maps.android.ui.IconGenerator(context)

        marker2.setTextAppearance(R.style.iconGenText2)
        marker2.setColor(Color.parseColor("#1266ed"))

        return BitmapDescriptorFactory.fromBitmap(marker2.makeIcon("${cluster.items.size}x"))
    }


    override fun getClusterItemIcon(clusterItem: MyItem): BitmapDescriptor {
        val marker2 = com.google.maps.android.ui.IconGenerator(context)

        marker2.setTextAppearance(R.style.iconGenText)
        if (clusterItem.banister == -1) {
            marker2.setColor(Color.parseColor("#f2d5d6"))
        } else {
            marker2.setColor(Color.parseColor("#e0e0ff"))
        }

        return BitmapDescriptorFactory.fromBitmap(marker2.makeIcon(clusterItem.name))
    }
}
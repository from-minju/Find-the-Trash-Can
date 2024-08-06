package org.techtown.trashcan

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class NearBinListLVAdapter(val nearbyBin: MutableList<TrashModel?>) : BaseAdapter() {

    override fun getCount(): Int {
        return nearbyBin.size
    }

    override fun getItem(p0: Int): TrashModel? {
        return nearbyBin[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView

        if(view == null) {

            view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item, parent, false)
        }

        try{


            var title = view?.findViewById<TextView>(R.id.titleArea)
            val content = view?.findViewById<TextView>(R.id.contentArea)

            title!!.text = nearbyBin[position]?.add.toString() +
                    " (" + nearbyBin[position]?.add_type.toString() + ")"


        }catch (e: java.lang.Exception){
            Log.d("결과", e.toString())
        }

        return view!!


    }
}
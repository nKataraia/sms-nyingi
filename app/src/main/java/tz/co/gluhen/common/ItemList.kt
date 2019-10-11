package tz.co.gluhen.common

import android.util.SparseArray
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener


class ItemList<T>(val recyclerView: RecyclerView,
                  private val itemViewCreator: (t:T)->RecyclerView.ViewHolder,
                  private val itemViewDataSetter:(sn:Int,t:T, r:RecyclerView.ViewHolder)->Unit
                ): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

   private val  TAG="ItemList"
   private val data= SparseArray<T>()
   init {
       setHasStableIds(true)
       val context=recyclerView.context
           recyclerView.layoutManager=LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)
           recyclerView.itemAnimator=DefaultItemAnimator()
           recyclerView.adapter=this
           recyclerView.addOnScrollListener(getScrollListener())
    }

   fun addItem(id:Int,item:T){
       data.put(id,item)
       this.notifyItemChanged(data.indexOfKey(id))
   }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
          return itemViewCreator.invoke(data[viewType])
    }

    override fun getItemViewType(position: Int): Int {
        return data.keyAt(position)
    }
    override fun getItemId(position: Int): Long {
        return data.keyAt(position).toLong()
    }

    override fun getItemCount(): Int { return data.size()}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
           itemViewDataSetter.invoke(position,data[data.keyAt(position)],holder)
    }


   private fun getScrollListener():OnScrollListener{
    return object :OnScrollListener(){
         override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
             super.onScrollStateChanged(recyclerView, newState)
             RecyclerView.SCROLL_STATE_DRAGGING
         }

         override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
             super.onScrolled(recyclerView, dx, dy)
//              Log.e(tag,"scrolled by (${dx},${dy})")
         }
     }}

    lateinit var itemClicked:(vh:RecyclerView.ViewHolder)->Unit
    lateinit var itemSwiped:(vh:RecyclerView.ViewHolder)->Unit
    lateinit var itemLongPressed:(vh:RecyclerView.ViewHolder)->Unit
}
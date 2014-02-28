package com.ericsson.nio

import java.nio.channels.Selector
import java.nio.channels.SelectionKey
import java.util.ConcurrentModificationException

/**
 * a dedicated thread ONLY to accept the connection requests.
 */
object NIOAcceptor extends Runnable {
    val acceptSel:Selector = Selector.open
    
	def run	{
	  while(acceptSel.isOpen()){
          try{
              val retKeys = acceptSel.select(100);
	          if(retKeys > 0)	{
	            val selectedKeys = acceptSel.selectedKeys();
	            if(selectedKeys.isEmpty() == false)	{
	            	val it:java.util.Iterator[SelectionKey] = selectedKeys.iterator();
		            while(it.hasNext()){
			           val sk:SelectionKey = it.next();
			           it.remove();
			           try{
			              if(sk.isAcceptable()){
			                val r:Runnable = sk.attachment().asInstanceOf[Runnable];
			                if(r != null) r.run();
			                else println("acceptor is null!");
			              }
		              } catch {
		                case e:Exception => {e.printStackTrace();try {sk.cancel(); sk.channel().close();} catch{case e:Exception=>{e.printStackTrace()}}}
		              }
		            }
	             }
	          }
          } catch {
            case e:ConcurrentModificationException => { e.printStackTrace();}
          }
        }
      
	}
}
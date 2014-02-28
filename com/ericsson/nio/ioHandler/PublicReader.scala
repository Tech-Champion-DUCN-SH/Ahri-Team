package com.ericsson.nio.ioHandler

import java.nio.channels.SelectionKey
import scala.collection.mutable.ArrayBuffer
import com.ericsson.nio.NIOServer
import com.ericsson.http.MonoHandler


class PublicReader() extends BaseIOHandler() {
    def this(tsk:SelectionKey) = {this();sk = tsk;}
    var monoHandler = new MonoHandler;
	def run	{
	  var len = -1;
	  bytes.clear;
	  try{
	    while(sc.isConnected() && {len = sc.read(buffer); len} > 0){
	      buffer.flip();
	      val content:Array[Byte] = new Array[Byte](buffer.limit());
	      buffer.get(content);
	      bytes.appendAll(content.toArray[Byte]);
	      buffer.clear();
	    }
	    if(len == 0)	{
	      //handle data receiving from external (Only proxy will do it)
	      
	        monoHandler.sk = sk;
	        monoHandler.headersBytes = bytes.toArray;
	        
	        NIOServer.executor.submit(monoHandler);
	     
	      
	      sk.selector().wakeup();
	    } else {
	      disconnect;
	    } 
	  }catch 	{
	      case e:Exception => {disconnect; e.printStackTrace();}
	  }
	}
	
	
}
package com.ericsson.nio.ioHandler

import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import com.ericsson.http.MonoHandler
import scala.collection.mutable.ArrayBuffer

abstract class BaseIOHandler() extends Runnable {
    def this(tsk:SelectionKey) = {this();sk=tsk;}
    
    var sk:SelectionKey = null;
	val BUFFER_SIZE = 4096
	var sc ={
	  if(sk != null)
	    sk.channel().asInstanceOf[SocketChannel];
	  else
	    null;
	} 
    var buffer = ByteBuffer.allocate(BUFFER_SIZE);
    
    var bytes:ArrayBuffer[Byte] = new ArrayBuffer[Byte];
    
    protected def disconnect	{
      try{
        //println("disconnected");
        sk.cancel();
        sc.close();
        sk.selector().wakeup();
        
      } catch {
        case e:Exception => {e.printStackTrace()}
      }
      //notify listener if needed
      val handlers = getHandlers;
      if(handlers.closer != null){
        handlers.closer.run();
      }
      
      sk.attach(null);
    }
    
    protected def getHandlers:SocketAttachment = {
      return sk.attachment().asInstanceOf[SocketAttachment];
    }
    
    def init(tsk:SelectionKey)	{
      sk=tsk;
    
      sc=tsk.channel().asInstanceOf[SocketChannel];
      buffer.clear;
      bytes.clear;
    }
}
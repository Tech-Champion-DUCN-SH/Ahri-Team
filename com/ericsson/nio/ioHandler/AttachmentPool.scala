package com.ericsson.nio.ioHandler
import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.channels.SelectionKey

object AttachmentPool {
	val SIZE = 2048;
	
	val queue = new ConcurrentLinkedQueue[SocketAttachment]();
	for(i <- 0 until SIZE)	{
	  val att = new SocketAttachment;
	  att.closer = new PublicCloserHandler(att);
	  queue.add(att);
	}
	  
	
	
	def getAttach(tsk:SelectionKey):SocketAttachment	=	{
	  var att = queue.poll();
	  if(att == null){
	    att = new SocketAttachment;
	    att.closer = new PublicCloserHandler(att);
	    //println("adding a new handler");
	  }
	  att.init(tsk);
	  //println("getting handlers:" + queue.size());
	  return att;
	}
	
	def retAttach(att:SocketAttachment)	{
	  queue.add(att);
	  //println("returning handlers:" + queue.size());
	}
}
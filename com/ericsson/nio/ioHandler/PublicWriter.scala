package com.ericsson.nio.ioHandler

import java.nio.channels.SelectionKey
import java.util.concurrent.ConcurrentLinkedQueue
import java.nio.ByteBuffer

class PublicWriter() extends BaseIOHandler() {
    var disconnectAfterSent:Boolean = false;
	val sendQueue = new ConcurrentLinkedQueue[ByteBuffer];
	var currentContent:ByteBuffer = null;//current sending buffer
	
	def this(tsk:SelectionKey) = {this();sk=tsk;disconnectAfterSent=true}
	def this(tsk:SelectionKey,td:Boolean) = {this();sk=tsk;disconnectAfterSent=td}
	def run{	
	  //check if current content sending is completed or not
	  var content:ByteBuffer = null;
	  if(currentContent != null && currentContent.hasRemaining())
	    content = currentContent;
	  else if(sendQueue.isEmpty()){ //nothing to send
	    currentContent = null;
	    sk.interestOps(SelectionKey.OP_READ);
	    sk.selector().wakeup();
	  } else	{
	    //there is something to send
	    currentContent = sendQueue.remove();
	    content = currentContent;
	  }
	  
	  if(content != null && content.hasRemaining())	{
		 try{
		   var len = sc.write(content);
		   //println("writing " + len + " bytes , content has " + content.remaining() + " remaining.");
		   if(len < 0)	{ //fault has find in sent
		     println("disconnect after send fault");
	         disconnect;
		   }
	       else {
	         if(content.hasRemaining() == false && sendQueue.size() == 0) {//nothing left to send
		         if(disconnectAfterSent)	{
		           //println("disconnect after sent");
		           disconnect;
		         }
		         else	{
		           //println("chagnes to reading mode ");
		           currentContent = null;
		           sk.interestOps(SelectionKey.OP_READ);
		           sk.selector().wakeup();
		         }
	       } else	{ //still has something to send..
	         sk.interestOps(SelectionKey.OP_WRITE);
	         sk.selector().wakeup();
	       }
	         
	      }
		 } catch 	{
		   case e:Exception => {disconnect;e.printStackTrace();}
		 }
	  } else	{
	    if(disconnectAfterSent)	{
	      //println("disconnect after sent2");
	       disconnect;
	     }
	     else	{
	       currentContent = null;
	       sk.interestOps(SelectionKey.OP_READ);
	       sk.selector().wakeup();
	    }
	  }
	}
	
	def sendData(data:ByteBuffer)	{
	  val copiedData = ByteBuffer.allocate(data.remaining());
	  //println("sending data : " + data.remaining());
	  copiedData.put(data);
	  copiedData.flip();
	  sendQueue.add(copiedData);
	  sk.interestOps(SelectionKey.OP_WRITE);
	  sk.selector().wakeup();
	}
}
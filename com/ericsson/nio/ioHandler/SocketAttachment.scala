package com.ericsson.nio.ioHandler
import scala.reflect._
import java.nio.channels.SelectionKey

class SocketAttachment() {
	var reader:BaseIOHandler = new PublicReader();
	var writer:PublicWriter = new PublicWriter();
	var closer:Runnable = null;
	var connecter:Runnable = null;
	@volatile var totalTransaction:Int = 0;
	@volatile var totalRecords = 0;
	
	def this(tsk:SelectionKey)=	{
	  this();
	  this.reader.sk = tsk;
	  this.writer.sk = tsk;
	}
	
	def init(tsk:SelectionKey)	{
	  reader.init(tsk);
	  writer.init(tsk);
	}
}
package com.ericsson.nio.ioHandler

import java.nio.channels.SelectionKey

class PublicCloserHandler(attach:SocketAttachment) extends Runnable {
	def run	{
	  AttachmentPool.retAttach(attach);
	}
}
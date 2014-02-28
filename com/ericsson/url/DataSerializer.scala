package com.ericsson.url

import java.util.HashMap
import java.io.PrintWriter
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.File
import java.util.concurrent.locks.ReentrantLock

object DataSerializer extends Runnable {
	val MAX_SIZE = 500
	val queue:HashMap[String,String] = new HashMap[String,String];
	val lock = new ReentrantLock;
	
	var unFull = lock.newCondition;
	var unEmp = lock.newCondition;
	
	
	def put(key:String,value:String)	{
	  lock.lock();
	  try	{
	    while(queue.size() == MAX_SIZE)	{
	      //println("Writing queue is full");
	      unFull.await();
	    }
	    
	    queue.put(key, value);
	    
	    unEmp.signal();
	  } catch {
	    case e:InterruptedException => e.printStackTrace();
	  } finally	{
	    lock.unlock();
	  }
	}
	
	def save()	{
	  lock.lock();
	  
	  try	{
	    while(queue.size() == 0){
	      unEmp.await();
	    }
	    
	    //save to file
	    flushDisk();
	    queue.clear();
	    unFull.signal();
	  } catch 	{
	  	case e:InterruptedException => e.printStackTrace();
	  }finally	{
	    lock.unlock();
	  }
	}
	
	def flushDisk()	{
	  val out = new PrintWriter(new FileOutputStream(new File("database.txt" ), true));
	  val keySet = queue.keySet();
	  val it = keySet.iterator()
	  while(it.hasNext()){
	    val key = it.next();
	    out.println(key+'`'+queue.get(key))
	  }
	  out.flush();
	  out.close()
	}
	
	def run {
	  while(true)	{
	    try{
	      Thread.sleep(5000);
	    } catch 	{
	      case e:InterruptedException => e.printStackTrace();
	    }
	    
	    save();
	  }
	}
}
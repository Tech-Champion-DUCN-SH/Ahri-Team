package com.ericsson.nio
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean
import java.nio.ByteBuffer
import com.ericsson.nio.ioHandler.SocketAttachment
import java.nio.channels.SocketChannel
import java.lang.management.MemoryMXBean
import java.io.LineNumberReader
import java.io.InputStreamReader
import java.util.concurrent.ThreadLocalRandom
import java.io.PrintWriter
import java.io.FileOutputStream
import java.io.File
import com.ericsson.nio.ioHandler.AttachmentPool
import com.ericsson.url.DataSerializer

object ClusterWatchDog extends Runnable {
    val CPU_THREA = 0.9;
    val MEM_THREA = 0.9;
    val REFRESH_INTERVAL = 5000;
    val opBean = ManagementFactory.getOperatingSystemMXBean();
    val r = Runtime.getRuntime;   
    var cpuRatio:Double = 0;
    var memRatio:Double = 0;
	
	val isWindows = System.getProperties().getProperty("os.name").toLowerCase().contains("windows");
	val windowsProcCmd = System.getenv("windir")  + "\\system32\\wbem\\wmic.exe process get Caption,CommandLine,KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperationCount";
	val CPU_TIME = 500;
	
	private var rejectPercent:Int = 0;
	
	
	def run	{
	  println("Starting watch dog...");
	  val statistic = new PrintWriter(new FileOutputStream(new File("stats.txt" ), true));
	  
	  var cpuAlarmSent = false;
	  var memAlarmSent = false;
	  //val startRecordNumber = DataSerializer
	  while(true)	{
	    
	    //judge system status
	    cpuRatio = getUsedCpuRatio;
	    memRatio = getUsedMemoryRatio;
	    //println("cpu is " + cpuRatio + "					memory is " + memRatio);
	    if(cpuRatio > CPU_THREA )	{
	      if(rejectPercent < 100)
	        rejectPercent += 5;
	      if(cpuAlarmSent == false){
	    	  cpuAlarmSent = true;
	    	  println("!!!WARNING CPU REJECTING TRAFFIC!");
	      }
	    } else {
	    	if(rejectPercent > 0 )
	    		rejectPercent -= 5;

	    	if(cpuAlarmSent  && rejectPercent == 0){
	    		println("CEASED : !!!WARNING CPU REJECTING TRAFFIC!");
	    		cpuAlarmSent = false;
	    	}
	    }

	   //println(rejectPercent + "currentCPU is " + cpuRatio);
	    
	    if(memRatio > MEM_THREA && memAlarmSent == false)	{
	      memAlarmSent = true;
	      println("!!!WARNING MEMORY FULL!");
	    }
	    
	    if(memRatio <= MEM_THREA && memAlarmSent == true)	{
	      memAlarmSent = false;
	      println("CEASED: !!!WARNING MEMORY FULL!");
	    }
	    
	    //collect stats of records and transactions;
	    var records = 0;
	    var transactions = 0;
	    val it = AttachmentPool.queue.iterator();
	    while(it.hasNext())	{
	      val attach = it.next();
	      records += attach.totalRecords;
	      transactions += attach.totalTransaction;
	    }
	    
	    statistic.println(transactions + " " + records + " " + cpuRatio + " " + ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
	    statistic.flush();
	    try{
	      Thread.sleep(REFRESH_INTERVAL);
	    } catch	{
	      case e:Exception=>{e.printStackTrace()}
	    }
	  } 
	}
	
	def getUsedMemoryRatio():Double	= {
	  val memManagement:MemoryMXBean  = ManagementFactory.getMemoryMXBean();
	  val memUsage = memManagement.getHeapMemoryUsage();
	  return memUsage.getUsed().asInstanceOf[Double] / memUsage.getMax();
	}
	
	def getUsedCpuRatio():Double = 	{
	  if(isWindows == false)
		  return opBean.getSystemLoadAverage();
	  else	{
	    var c0 = windowsReadCpu(Runtime.getRuntime().exec(windowsProcCmd));
	    Thread.sleep(CPU_TIME);
	    var c1 = windowsReadCpu(Runtime.getRuntime().exec(windowsProcCmd));
	    
	    if(c0 != null && c1 != null){
	      val idletime = c1(0) - c0(0);
	      val busytime = c1(1) - c0(1);
	      
	      return busytime * 1.0 / (busytime + idletime);
	      
	    } else	{
	      return 0;
	    }
	  }
	}
	
	def windowsReadCpu(proc:Process):Array[Long] = {
	  val ret = new Array[Long](2);
	  try{
	    proc.getOutputStream().close();
	    val input = new LineNumberReader(new InputStreamReader(proc.getInputStream()));
	    var line = input.readLine();
	    
	    if ((line == null) || (line.length() < 10)) {  
           return null;  
        }  
	    
	    val capidx = line.indexOf("Caption");
	    val cmdidx = line.indexOf("CommandLine");  
        val rocidx = line.indexOf("ReadOperationCount");  
        val umtidx = line.indexOf("UserModeTime");  
        val kmtidx = line.indexOf("KernelModeTime");  
        val wocidx = line.indexOf("WriteOperationCount"); 
        var idletime:Long = 0;
        var kneltime:Long = 0;
        var usertime:Long = 0;
        
        while({line=input.readLine();line} != null){
          if (line.length() >= wocidx) {  
             val caption = line.substring(capidx, cmdidx-1).trim();
             val cmd = line.substring(cmdidx, kmtidx-1).trim();
             
             if(cmd.indexOf("wmic.exe") < 0)	{ //remove wmic process
               val s1 = line.substring(kmtidx, rocidx-1).trim();
               val s2 = line.substring(umtidx, wocidx-1).trim();
               
               if(caption.equals("System Idle Process")  || caption.equals("System"))	{
                 if(s1.length() > 0)
                   idletime += + s1.toLong;
                 
                 if(s2.length() > 0)
                   idletime += s2.toLong;
               } else	{
                 if(s1.length() > 0)
                   kneltime += + s1.toLong;
                 
                 if(s2.length() > 0)
                   usertime += s2.toLong;
               }
             }
          }
        }
        ret(0) = idletime;
        ret(1) = kneltime + usertime;
        return ret;
	  } catch	{
	    case e:Exception	=>	{e.printStackTrace();}
	  } finally	{
	    try{proc.getInputStream().close();} catch{case e:Exception => {e.printStackTrace()}}
	  }
	  return null;
	}
	
	def cpuOverloaded:Boolean = {
	  if(rejectPercent > 0)	{
	    val r = ThreadLocalRandom.current().nextInt(0, 100);
	    r < rejectPercent;
	  }else	{
	    false;
	  }
	}
	
	def memOverloaded:Boolean = {
	  return memRatio > MEM_THREA;
	}
}
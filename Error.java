Generating CRD Files...
java.lang.NullPointerException: Cannot invoke "org.json.JSONArray.length()" because "jsonArray" is null
	at com.JSONtoExcelApplication.odm_CDH_cdhIPLst.process(odm_CDH_cdhIPLst.java:99)
	at com.JSONtoExcelApplication.ExcelGenerator.processJsonObject(ExcelGenerator.java:100)
	at com.JSONtoExcelApplication.ExcelGenerator.generateExcelFile(ExcelGenerator.java:35)
	at com.JSONtoExcelApplication.aMainRun.processJsonRequest(aMainRun.java:236)
	at com.JSONtoExcelApplication.aMainRun.runConversionWithOutputDirectory(aMainRun.java:155)
	at com.JSONtoExcelApplication.aMainRun.runConversion(aMainRun.java:75)
	at com.JSONtoExcelApplication.UserInputUI$4.lambda$0(UserInputUI.java:443)
	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
	at java.base/java.lang.Thread.run(Thread.java:833)

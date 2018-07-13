package org.camunda.bpm.platform.plugin.optimize.data.preparation;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.impl.history.event.HistoricActivityInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.producer.CacheAwareHistoryEventProducer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class OptimizeAdaptionHistoryEventProducer extends CacheAwareHistoryEventProducer {

  private Random random = new Random();

  private long seconds = 1000;
  private long minutes = 60 * seconds;
  private long hours = 60 * minutes;
  private long days = 24 * hours;
  private long weeks = 7 * days;
  private long months = 30 * days;
  private long years = 12 * months;

  // Assumption: we only create 4500 process instances
  private final int processInstanceCount = 4500;
  private LinkedList<Date> dateDistribution = createSortedStartDateDistribution();

  private LinkedList<Date> createSortedStartDateDistribution() {
    LinkedList<Date> dateDistribution = new LinkedList<Date>();
    for(int i=0; i< processInstanceCount; i++){
      dateDistribution.addLast(drawStartDateFromDistribution());
    }
    Collections.sort(dateDistribution);
    return dateDistribution;
  }


  private Date calculateDateForOneYearFromNow() {
    long desiredStandardDeviation = 2 * months;
    long desiredMean = 1 * years;
    long desired = Math.round(random.nextGaussian()*desiredStandardDeviation)+desiredMean;
    desired = Math.max(desired, -desiredMean);
    long now = new Date().getTime();
    return new Date(now - desired);
  }

  private Date calculateDateForSixMonthFromNow() {
    long desiredStandardDeviation = 1 * weeks;
    long desiredMean = 6 * months;
    long desired = Math.round(random.nextGaussian()*desiredStandardDeviation)+desiredMean;
    desired = Math.max(desired, -desiredMean);
    long now = new Date().getTime();
    return new Date(now - desired);
  }

  private Date calculateDateForOneMonthFromNow() {
    long desiredStandardDeviation =  1 * weeks;
    long desiredMean = 1 * months;
    long desired = Math.round(random.nextGaussian()*desiredStandardDeviation)+desiredMean;
    desired = Math.max(desired, -1*months);
    long now = new Date().getTime();
    return new Date(now - desired);
  }

  private Date calculateDateForLastWeek() {
    long desiredStandardDeviation = 2 * days;
    long desiredMean = 1*weeks;
    long desired = Math.round(random.nextGaussian()*desiredStandardDeviation)+desiredMean;
    desired = Math.max(desired, -1*weeks);
    long now = new Date().getTime();
    return new Date(now - desired);
  }

  private Date calculateDateForYesterday() {
    long desiredStandardDeviation = 1 * days;
    long desiredMean = 1 * days;
    long desired = Math.round(random.nextGaussian()*desiredStandardDeviation)+desiredMean;
    desired = Math.abs(desired);
    long now = new Date().getTime();
    return new Date(now - desired);
  }

  private int chooseBucketWhenToCreateStartDate() {
    int bucket = random.nextInt(5);
    return bucket;
  }

  private Date drawStartDateFromDistribution() {
    int bucket = chooseBucketWhenToCreateStartDate();
    switch (bucket) {
      case 0:
        return calculateDateForOneYearFromNow();
      case 1:
        return calculateDateForSixMonthFromNow();
      case 2:
        return calculateDateForOneMonthFromNow();
      case 3:
        return calculateDateForLastWeek();
      case 4:
        return calculateDateForYesterday();
    }
    return new Date();
  }

  @Override
  public HistoryEvent createProcessInstanceStartEvt(DelegateExecution execution) {
    HistoricProcessInstanceEventEntity event =
      (HistoricProcessInstanceEventEntity) super.createProcessInstanceStartEvt(execution);
    Date startDate = dateDistribution.isEmpty()? new Date(): dateDistribution.removeFirst();
    event.setStartTime(startDate);
    return event;
  }

  private Date getEndTime(DelegateExecution execution) {
	  HistoryService historyService = execution.getProcessEngineServices().getHistoryService();
	  List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
			  .processInstanceId(execution.getProcessInstanceId()).list();
	  long duration = 0;
	  for (HistoricActivityInstance activityInstance : activityInstances) {
		  if (activityInstance.getDurationInMillis() == null) {
			 Date start = activityInstance.getStartTime();
			 Date end = activityInstance.getEndTime();
			 if (end != null) {
				 duration += (end.getTime() - start.getTime());
			 }
		 } else {
			 duration += activityInstance.getDurationInMillis();
		 }
	  }
	  HistoricProcessInstance processInstance = historyService
			  	.createHistoricProcessInstanceQuery()
			  	.processInstanceId(execution.getProcessInstanceId())
			  	.singleResult();
	  if (processInstance != null) {
		  Date startTime = processInstance.getStartTime();
		  return new Date(startTime.getTime() + duration);
	  } else {
		  return new Date();
	  }
  }

  @Override
  public HistoryEvent createProcessInstanceEndEvt(DelegateExecution execution) {
	  HistoricProcessInstanceEventEntity event =
      (HistoricProcessInstanceEventEntity) super.createProcessInstanceEndEvt(execution);

	boolean executeEndTime = false;

    Set<String> names = execution.getVariableNames();
    for (String name : names ) {
    		if (name.contains("Task_")) {
    			execution.removeVariable(name);
    			executeEndTime = true;
    		}
    }

    if (executeEndTime) {
    		//setting end time
    		event.setEndTime(getEndTime(execution));
    }

    return event;
  }

  @Override
  public HistoryEvent createActivityInstanceEndEvt(DelegateExecution execution) {
    HistoricActivityInstanceEventEntity event =
      (HistoricActivityInstanceEventEntity) super.createActivityInstanceEndEvt(execution);
    String taskId = event.getActivityId();
    if(execution.getVariable(taskId) != null) {
      Long duration = (Long) execution.getVariable(taskId);
      event.setDurationInMillis(duration);
      execution.removeVariable(taskId);
    }
    return event;
  }
}

package org.camunda.bpm.platform.plugin.optimize.data.preparation;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.history.event.HistoricActivityInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.producer.CacheAwareHistoryEventProducer;

import java.util.Date;
import java.util.Random;

public class OptimizeAdaptionHistoryEventProducer extends CacheAwareHistoryEventProducer {

  private Random random = new Random();

  private long seconds = 1000;
  private long minutes = 60 * seconds;
  private long hours = 60 * minutes;
  private long days = 24 * hours;
  private long weeks = 7 * days;
  private long months = 30 * days;
  private long years = 12 * months;

  // two years, one year, six month, one month, last week, yesterday

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
    event.setStartTime(drawStartDateFromDistribution());
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

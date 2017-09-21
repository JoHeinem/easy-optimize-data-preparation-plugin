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

  private long calculateDateDistribution(long mean, long deviation) {
    return Math.abs(Math.round(random.nextGaussian() * deviation) + mean);
  }

  private int chooseBucketWhenToCreateDayOfYear() {
    double bucket = random.nextDouble();
    if (bucket <= 0.35) {
      return 0;
    } else if (bucket <= 0.85) {
      return 1;
    } else {
      return 2;
    }
  }

  private long drawSpringNormalDistributionSample() {
    return Math.abs(calculateDateDistribution(104, 50));
  }

  private long drawFallNormalDistributionSample() {
    // TODO: wenn drüber dann gleichverteilung
    return Math.min(Math.abs(calculateDateDistribution(287, 50)), 365);
  }

  private long drawEqualDistribution() {
    return random.nextInt(365) + 1;
  }

  private long drawDayOfYearFromDistribution() {
    int bucket = chooseBucketWhenToCreateDayOfYear();
    switch (bucket) {
      case 0:
        return drawSpringNormalDistributionSample();
      case 1:
        return drawFallNormalDistributionSample();
      case 2:
        return drawEqualDistribution();
    }
    return 0;
  }

  @Override
  public HistoryEvent createProcessInstanceStartEvt(DelegateExecution execution) {
    HistoricProcessInstanceEventEntity event =
      (HistoricProcessInstanceEventEntity) super.createProcessInstanceStartEvt(execution);
    Date startTime = DateConverter.convertToDateOfCurrentYear(drawDayOfYearFromDistribution());
    event.setStartTime(startTime);
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
    }
    return event;
  }
}

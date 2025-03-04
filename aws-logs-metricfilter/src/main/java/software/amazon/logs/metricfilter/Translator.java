package software.amazon.logs.metricfilter;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchlogs.model.DeleteMetricFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeMetricFiltersRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeMetricFiltersResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidParameterException;
import software.amazon.awssdk.services.cloudwatchlogs.model.LimitExceededException;
import software.amazon.awssdk.services.cloudwatchlogs.model.OperationAbortedException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutMetricFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatchlogs.model.ServiceUnavailableException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

  static void translateException(final AwsServiceException e) {
    if (e instanceof InvalidParameterException) {
      throw new CfnInvalidRequestException(String.format("%s. %s", ResourceModel.TYPE_NAME, e.getMessage()), e);
    } else if (e instanceof LimitExceededException) {
      throw new CfnServiceLimitExceededException(e);
    } else if (e instanceof OperationAbortedException) {
      throw new CfnResourceConflictException(e);
    } else if (e instanceof ResourceNotFoundException) {
      throw new CfnNotFoundException(e);
    } else if (e instanceof ServiceUnavailableException) {
      throw new CfnServiceInternalErrorException(e);
    } else if (e instanceof ResourceAlreadyExistsException) {
      throw new CfnAlreadyExistsException(e);
    }
    throw new CfnGeneralServiceException(e);
  }

  static software.amazon.awssdk.services.cloudwatchlogs.model.MetricTransformation translateMetricTransformationToSdk
  (final software.amazon.logs.metricfilter.MetricTransformation metricTransformation) {
    if (metricTransformation == null) {
      return null;
    }

    software.amazon.awssdk.services.cloudwatchlogs.model.MetricTransformation metricTransformationSDKModel = software.amazon.awssdk.services.cloudwatchlogs.model.MetricTransformation.builder()
            .metricName(metricTransformation.getMetricName())
            .metricValue(metricTransformation.getMetricValue())
            .metricNamespace(metricTransformation.getMetricNamespace())
            .defaultValue(metricTransformation.getDefaultValue())
            .build();

    if (metricTransformation.getDimensions() != null && !metricTransformation.getDimensions().isEmpty()){
      HashMap<String, String> dimensionsMap = new HashMap<String, String>();
      for (Dimension entry: metricTransformation.getDimensions()) {
        dimensionsMap.put(entry.getKey(), entry.getValue());
      }
      metricTransformationSDKModel = metricTransformationSDKModel.toBuilder().dimensions(dimensionsMap).build();
    }

    if (metricTransformation.getUnit()!=null){
      metricTransformationSDKModel = metricTransformationSDKModel.toBuilder().unit(metricTransformation.getUnit()).build();
    }

    return metricTransformationSDKModel;
  }

  static software.amazon.logs.metricfilter.MetricTransformation translateMetricTransformationFromSdk
  (final software.amazon.awssdk.services.cloudwatchlogs.model.MetricTransformation metricTransformation) {
    if (metricTransformation == null) {
      return null;
    }

    software.amazon.logs.metricfilter.MetricTransformation metricTransformationLogsModel = software.amazon.logs.metricfilter.MetricTransformation.builder()
            .metricName(metricTransformation.metricName())
            .metricValue(metricTransformation.metricValue())
            .metricNamespace(metricTransformation.metricNamespace())
            .defaultValue(metricTransformation.defaultValue())
            .build();

    if (metricTransformation.hasDimensions()){
      Set<Dimension> dimensionsSet = new HashSet<Dimension>();

      for (String name: metricTransformation.dimensions().keySet()) {
        String key = name.toString();
        String value = metricTransformation.dimensions().get(name).toString();
        dimensionsSet.add(Dimension.builder().key(key).value(value).build());
      }
      metricTransformationLogsModel.setDimensions(dimensionsSet);
    }
    if (metricTransformation.unit() != null){
      metricTransformationLogsModel.setUnit(metricTransformation.unitAsString());
    }

    return metricTransformationLogsModel;
  }

  static List<software.amazon.logs.metricfilter.MetricTransformation> translateMetricTransformationFromSdk
          (final List<software.amazon.awssdk.services.cloudwatchlogs.model.MetricTransformation> metricTransformations) {
    if (metricTransformations.isEmpty()) {
      return null;
    }
    return metricTransformations.stream()
            .map(Translator::translateMetricTransformationFromSdk)
            .collect(Collectors.toList());
  }

  static ResourceModel translateMetricFilter
          (final software.amazon.awssdk.services.cloudwatchlogs.model.MetricFilter metricFilter) {
    List<MetricTransformation> mts = metricFilter.metricTransformations()
            .stream()
            .map(Translator::translateMetricTransformationFromSdk)
            .collect(Collectors.toList());
    return ResourceModel.builder()
            .filterName(metricFilter.filterName())
            .logGroupName(metricFilter.logGroupName())
            // When a filter pattern is "" the API sets it to null, but this is a meaningful pattern and the
            // contract should be identical to what our caller provided
            .filterPattern(metricFilter.filterPattern() == null ? "" : metricFilter.filterPattern())
            .metricTransformations(mts)
            .build();
  }

  static software.amazon.awssdk.services.cloudwatchlogs.model.MetricFilter translateToSDK
          (final ResourceModel model) {
    List<software.amazon.awssdk.services.cloudwatchlogs.model.MetricTransformation> mts = model.getMetricTransformations()
            .stream()
            .map(Translator::translateMetricTransformationToSdk)
            .collect(Collectors.toList());
    return software.amazon.awssdk.services.cloudwatchlogs.model.MetricFilter.builder()
            .filterName(model.getFilterName())
            .logGroupName(model.getLogGroupName())
            .filterPattern(model.getFilterPattern())
            .metricTransformations(mts)
            .build();
  }

  static software.amazon.awssdk.services.cloudwatchlogs.model.MetricTransformation translateToSDK
          (final MetricTransformation metricTransformation) {
    return translateMetricTransformationToSdk(metricTransformation);
  }

  static List<software.amazon.awssdk.services.cloudwatchlogs.model.MetricTransformation> translateMetricTransformationToSDK
          (final List<MetricTransformation> metricTransformations) {
    return metricTransformations.stream()
            .map(Translator::translateToSDK)
            .collect(Collectors.toList());
  }

  static PutMetricFilterRequest translateToCreateRequest(final ResourceModel model) {
    return PutMetricFilterRequest.builder()
            .logGroupName(model.getLogGroupName())
            .filterName(model.getFilterName())
            .filterPattern(model.getFilterPattern())
            .metricTransformations(model.getMetricTransformations()
                    .stream()
                    .map(Translator::translateMetricTransformationToSdk)
                    .collect(Collectors.toSet()))
            .build();
  }

  static DescribeMetricFiltersRequest translateToReadRequest(final ResourceModel model) {
    return DescribeMetricFiltersRequest.builder()
            .filterNamePrefix(model.getFilterName())
            .logGroupName(model.getLogGroupName())
            .limit(1)
            .build();
  }

  static ResourceModel translateFromReadResponse(final DescribeMetricFiltersResponse awsResponse) {
    return awsResponse.metricFilters()
            .stream()
            .map(Translator::translateMetricFilter)
            .findFirst()
            .get();
  }

  static DeleteMetricFilterRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteMetricFilterRequest.builder()
            .filterName(model.getFilterName())
            .logGroupName(model.getLogGroupName())
            .build();
  }

  static PutMetricFilterRequest translateToUpdateRequest(final ResourceModel model) {
    return translateToCreateRequest(model);
  }

  static DescribeMetricFiltersRequest translateToListRequest(final String nextToken) {
    return DescribeMetricFiltersRequest.builder()
            .nextToken(nextToken)
            .limit(50)
            .build();
  }

  static List<ResourceModel> translateFromListResponse(final DescribeMetricFiltersResponse awsResponse) {
    return streamOfOrEmpty(awsResponse.metricFilters())
            .map(Translator::translateMetricFilter)
            .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
  }
}

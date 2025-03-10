package software.amazon.logs.metricfilter;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CloudWatchLogsException;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeMetricFiltersRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeMetricFiltersResponse;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Objects;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchLogsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        logger.log("Trying to read resource...");

        return proxy.initiate("AWS-Logs-MetricFilter::Read", proxyClient, model, callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .backoffDelay(backoffStrategy)
            .makeServiceCall((awsRequest, sdkProxyClient) -> readResource(awsRequest, sdkProxyClient , model))
            .handleError(handleError)
            .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.SUCCESS)
                .resourceModel(Translator.translateFromReadResponse(awsResponse))
                .build());
    }

    private DescribeMetricFiltersResponse readResource(
            final DescribeMetricFiltersRequest awsRequest,
            final ProxyClient<CloudWatchLogsClient> proxyClient,
            final ResourceModel model) {
        DescribeMetricFiltersResponse awsResponse = null;
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeMetricFilters);
        } catch (CloudWatchLogsException e) {
            Translator.translateException(e);
        }

        if (awsResponse != null && awsResponse.metricFilters().isEmpty()) {
            logger.log("Resource does not exist.");
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, Objects.toString(model.getPrimaryIdentifier()));
        }

        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }
}

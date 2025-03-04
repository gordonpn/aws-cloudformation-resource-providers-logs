package software.amazon.logs.subscriptionfilter;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DeleteSubscriptionFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DeleteSubscriptionFilterResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private final String CALL_GRAPH_STRING = "AWS-Logs-SubscriptionFilter::Delete";

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudWatchLogsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        final String stackId = request.getStackId() == null ? "" : request.getStackId();

        logger.log(String.format("Invoking %s request for model: %s with StackID: %s", CALL_GRAPH_STRING, model, stackId));

        return proxy.initiate(CALL_GRAPH_STRING, proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((_request, _callbackContext) -> deleteResource(_request, proxyClient, stackId))
                .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.SUCCESS)
                        .build());
    }

    private DeleteSubscriptionFilterResponse deleteResource(
            final DeleteSubscriptionFilterRequest awsRequest,
            final ProxyClient<CloudWatchLogsClient> proxyClient,
            final String stackId) {
        DeleteSubscriptionFilterResponse deleteSubscriptionFilterResponse = null;
        try {
            deleteSubscriptionFilterResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::deleteSubscriptionFilter);
        } catch (Exception e) {
            handleException(e, logger, stackId);
        }

        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
        return deleteSubscriptionFilterResponse;
    }
}

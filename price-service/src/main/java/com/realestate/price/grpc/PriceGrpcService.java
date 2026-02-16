package com.realestate.price.grpc;

import com.realestate.grpc.price.*;
import com.realestate.price.dto.PriceHistoryResponse;
import com.realestate.price.dto.PricePointResponse;
import com.realestate.price.dto.PriceResponse;
import com.realestate.price.exception.PriceNotFoundException;
import com.realestate.price.service.PriceManagementService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class PriceGrpcService extends PriceServiceGrpc.PriceServiceImplBase {

    private final PriceManagementService priceService;

    @Override
    public void getCurrentPrice(PropertyId request, StreamObserver<Price> responseObserver) {
        try {
            PriceResponse price = priceService.getCurrentPrice(request.getPropertyId());
            responseObserver.onNext(toGrpcPrice(price));
            responseObserver.onCompleted();
        } catch (PriceNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in getCurrentPrice gRPC: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    @Override
    public void getPriceHistory(GetPriceHistoryRequest request, StreamObserver<PriceHistory> responseObserver) {
        try {
            PriceHistoryResponse history = priceService.getPriceHistory(request.getPropertyId());

            PriceHistory.Builder builder = PriceHistory.newBuilder()
                    .setPropertyId(request.getPropertyId());

            for (PricePointResponse point : history.pricePoints()) {
                PricePoint.Builder pointBuilder = PricePoint.newBuilder()
                        .setNewPrice(point.newPrice().toPlainString())
                        .setChangedAt(point.changedAt() != null ? point.changedAt().toString() : "");

                if (point.oldPrice() != null) {
                    pointBuilder.setOldPrice(point.oldPrice().toPlainString());
                }
                if (point.changedBy() != null) {
                    pointBuilder.setChangedBy(point.changedBy());
                }
                if (point.reason() != null) {
                    pointBuilder.setReason(point.reason());
                }

                builder.addPricePoints(pointBuilder.build());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (PriceNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in getPriceHistory gRPC: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    @Override
    public void updatePrice(UpdatePriceRequest request, StreamObserver<Price> responseObserver) {
        try {
            PriceResponse price = priceService.updatePriceInternal(
                    request.getPropertyId(),
                    new BigDecimal(request.getNewPrice()),
                    request.getCurrency().isEmpty() ? null : request.getCurrency(),
                    request.getChangedBy().isEmpty() ? null : request.getChangedBy(),
                    request.getReason().isEmpty() ? null : request.getReason()
            );

            responseObserver.onNext(toGrpcPrice(price));
            responseObserver.onCompleted();
        } catch (PriceNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (NumberFormatException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid price format: " + request.getNewPrice())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error in updatePrice gRPC: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    @Override
    public void batchGetPrices(BatchGetPricesRequest request, StreamObserver<BatchGetPricesResponse> responseObserver) {
        try {
            List<PriceResponse> prices = priceService.batchGetPrices(request.getPropertyIdsList());

            BatchGetPricesResponse.Builder builder = BatchGetPricesResponse.newBuilder();
            for (PriceResponse price : prices) {
                builder.addPrices(toGrpcPrice(price));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in batchGetPrices gRPC: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    private Price toGrpcPrice(PriceResponse price) {
        Price.Builder builder = Price.newBuilder()
                .setPropertyId(price.propertyId())
                .setCurrentPrice(price.currentPrice().toPlainString())
                .setCurrency(price.currency() != null ? price.currency() : "VND");

        if (price.lastUpdatedBy() != null) {
            builder.setLastUpdatedBy(price.lastUpdatedBy());
        }
        if (price.updatedAt() != null) {
            builder.setUpdatedAt(price.updatedAt().toString());
        }

        return builder.build();
    }
}

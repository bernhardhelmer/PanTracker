package io.everyonecodes.project_module.services;

import io.everyonecodes.project_module.dtos.requests.UsageLogRequest;
import io.everyonecodes.project_module.dtos.responses.UsageLogResponse;
import io.everyonecodes.project_module.exceptions.ResourceNotFoundException;
import io.everyonecodes.project_module.models.*;
import io.everyonecodes.project_module.repositories.ProductRepository;
import io.everyonecodes.project_module.repositories.ProjectProductRepository;
import io.everyonecodes.project_module.repositories.ProjectRepository;
import io.everyonecodes.project_module.repositories.UsageLogRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsageLogService {

    private final UsageLogRepository usageLogRepository;
    private final ProductRepository productRepository;
    private final ProjectRepository projectRepository;
    private final ProjectProductRepository projectProductRepository;

    // logs single usage and updates product weight and progress
    @Transactional
    public UsageLogResponse logUsage(Long productId, UsageLogRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found."));

        if (product.isFinished()) {
            throw new IllegalArgumentException("Cannot log usage for an item that is already finished.");
        }

        // synchronize Project Progress if in project
        Optional<Project> projectOpt = Optional.ofNullable(request.getProjectId())
                .map(projectId -> {
                    Project project = projectRepository.findById(projectId)
                            .orElseThrow(() -> new ResourceNotFoundException("Project with ID " + projectId + " not found."));

                    ProjectProductId junctionId = new ProjectProductId(projectId, productId);
                    if (!projectProductRepository.existsById(junctionId)) {
                        throw new IllegalArgumentException("Product with ID " + productId +
                                " is not participating in Project with ID " + projectId);
                    }
                    return project;
                });

        // check if weight is correct
        Optional.ofNullable(request.getWeightRecorded())
                .ifPresent(weight -> {
                    Optional.ofNullable(product.getCurrentWeightGrams())
                            .ifPresent(currentWeight -> {
                                if (weight.compareTo(currentWeight) > 0) {
                                    throw new IllegalArgumentException("Recorded weight (" + weight +
                                            "g) cannot be greater than the product's last recorded weight (" + currentWeight + "g).");
                                }
                            });

                    product.setCurrentWeightGrams(weight);

                    if (product.getCurrentWeightGrams().compareTo(BigDecimal.ZERO) <= 0) {
                        product.setFinished(true);
                    }
                });

        // save Usage log
        UsageLog log = UsageLog.builder()
                .product(product)
                .project(projectOpt.orElse(null))
                .useDate(Optional.ofNullable(request.getUseDate()).orElseGet(LocalDate::now))
                .weightRecorded(request.getWeightRecorded())
                .notes(request.getNotes())
                .build();

        UsageLog savedLog = usageLogRepository.save(log);

        productRepository.save(product);

        return mapToResponse(savedLog);
    }

    // get usage history of a product
    @Transactional(readOnly = true)
    public List<UsageLogResponse> getProductUsageHistory(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found.");
        }

        return usageLogRepository.findByProductIdOrderByUseDateDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // delete usage log and reset product
    @Transactional
    public void deleteUsageLog(Long logId) {
        UsageLog log = usageLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Usage log with ID " + logId + " not found."));

        Product product = log.getProduct();

        usageLogRepository.delete(log);
    }

    private UsageLogResponse mapToResponse(UsageLog log) {
        return UsageLogResponse.builder()
                .id(log.getId())
                .productId(log.getProduct().getId())
                .productName(log.getProduct().getName())
                .projectId(log.getProject() != null ? log.getProject().getId() : null)
                .projectName(log.getProject() != null ? log.getProject().getName() : null)
                .useDate(log.getUseDate())
                .weightRecorded(log.getWeightRecorded())
                .notes(log.getNotes())
                .build();
    }
}

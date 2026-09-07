package io.everyonecodes.project_module.services;

import io.everyonecodes.project_module.dtos.requests.ProjectProductLinkRequest;
import io.everyonecodes.project_module.dtos.responses.ProjectProductResponse;
import io.everyonecodes.project_module.exceptions.ResourceNotFoundException;
import io.everyonecodes.project_module.models.Product;
import io.everyonecodes.project_module.models.Project;
import io.everyonecodes.project_module.models.ProjectProduct;
import io.everyonecodes.project_module.models.ProjectProductId;
import io.everyonecodes.project_module.repositories.ProductRepository;
import io.everyonecodes.project_module.repositories.ProjectProductRepository;
import io.everyonecodes.project_module.repositories.ProjectRepository;
import io.everyonecodes.project_module.repositories.UsageLogRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectProductService {
    private final ProjectProductRepository projectProductRepository;
    private final ProjectRepository projectRepository;
    private final ProductRepository productRepository;
    private final UsageLogRepository usageLogRepository;

    // link product to project and set goals for usage
    @Transactional
    public ProjectProductResponse addProductToProject(Long projectId, Long productId, ProjectProductLinkRequest request) {
        // check if project and product exist
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with ID " + projectId + " not found."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found."));

        ProjectProductId id = new ProjectProductId(projectId, productId);

        if (projectProductRepository.existsById(id)) {
            throw new IllegalArgumentException("Product is already linked to this project.");
        }

        ProjectProduct projectProduct = ProjectProduct.builder()
                .id(id)
                .project(project)
                .product(product)
                .goalType(request.getGoalType())
                .targetUses(request.getTargetUses())
                .build();

        ProjectProduct saved = projectProductRepository.save(projectProduct);
        return mapToResponse(saved);
    }

    @Transactional
    public void removeProductFromProject(Long projectId, Long productId) {
        ProjectProductId id = new ProjectProductId(projectId, productId);

        ProjectProduct projectProduct = projectProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Link between Project " + projectId + " and Product " + productId + " not found."));

        projectProductRepository.delete(projectProduct);
    }

    @Transactional(readOnly = true)
    public List<ProjectProductResponse> getProductsInProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with ID " + projectId + " not found.");
        }

        return projectProductRepository.findByProjectId(projectId)
                .stream()
                .sorted(Comparator.comparing((ProjectProduct jp) -> jp.getProduct().getCategory().getName())
                        .thenComparing(jp -> jp.getProduct().getOpeningDate()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectProductResponse> getProjectsForProduct(Long productId) {
        return projectProductRepository.findByProductId(productId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProjectProductResponse mapToResponse(ProjectProduct junction) {
        Long productId = junction.getProduct().getId();
        Long projectId = junction.getProject().getId();
        int currentUses = usageLogRepository.countByProductIdAndProjectId(productId, projectId);

        return ProjectProductResponse.builder()
                .projectId(projectId)
                .projectName(junction.getProject().getName())
                .productId(productId)
                .productName(junction.getProduct().getName())
                .productBrand(junction.getProduct().getBrand())
                .categoryName(junction.getProduct().getCategory().getName())
                .currentWeightGrams(junction.getProduct().getCurrentWeightGrams())
                .isFinished(junction.getProduct().isFinished())
                .goalType(junction.getGoalType())
                .targetUses(junction.getTargetUses())
                .currentUses(currentUses)
                .build();
    }
}

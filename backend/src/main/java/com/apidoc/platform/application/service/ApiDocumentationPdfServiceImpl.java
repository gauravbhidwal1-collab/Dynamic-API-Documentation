package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiDefinitionRequest;
import com.apidoc.platform.domain.exception.DomainException;
import com.apidoc.platform.infrastructure.pdf.ItextApiDocumentationPdfGenerator;
import com.apidoc.platform.infrastructure.persistence.entity.ApiField;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.apidoc.platform.infrastructure.persistence.entity.ApiResponseField;
import com.apidoc.platform.infrastructure.persistence.repository.ApiMasterRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiDocumentationPdfServiceImpl implements ApiDocumentationPdfService {

    private final ApiMasterRepository apiMasterRepository;
    private final ApiMasterVersionResolver apiMasterVersionResolver;
    private final ItextApiDocumentationPdfGenerator pdfGenerator;
    private final ApiDefinitionHydrator apiDefinitionHydrator;

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long apiId) {
        return generatePdf(apiId, false, null);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long apiId, boolean compact) {
        return generatePdf(apiId, compact, null);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long apiId, boolean compact, String version) {
        ApiMaster master = apiMasterVersionResolver.resolve(apiId, version);
        initializeFieldTrees(master);
        try {
            return pdfGenerator.generate(master, compact);
        } catch (IOException e) {
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate PDF");
        }
    }

    private void initializeFieldTrees(ApiMaster master) {
        master.getRequestFields().size();
        master.getResponseFields().size();
        master.getRequestFields().stream()
                .filter(f -> f.getParent() == null)
                .forEach(this::walkRequestField);
        master.getResponseFields().stream()
                .filter(f -> f.getParent() == null)
                .forEach(this::walkResponseField);
    }

    private void walkRequestField(ApiField field) {
        field.getChildFields().size();
        for (ApiField c : field.getChildFields()) {
            walkRequestField(c);
        }
    }

    private void walkResponseField(ApiResponseField field) {
        field.getChildFields().size();
        for (ApiResponseField c : field.getChildFields()) {
            walkResponseField(c);
        }
    }

    @Override
    public byte[] generatePreviewPdf(ApiDefinitionRequest request) {
        return generatePreviewPdf(request, false);
    }

    @Override
    public byte[] generatePreviewPdf(ApiDefinitionRequest request, boolean compact) {
        ApiDefinitionRequest normalized = ApiDefinitionPdfPreviewNormalizer.normalize(request);
        ApiMaster master = new ApiMaster();
        try {
            apiDefinitionHydrator.hydrate(master, normalized);
            return pdfGenerator.generate(master, compact);
        } catch (DomainException e) {
            throw e;
        } catch (IOException e) {
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate PDF preview");
        }
    }
}

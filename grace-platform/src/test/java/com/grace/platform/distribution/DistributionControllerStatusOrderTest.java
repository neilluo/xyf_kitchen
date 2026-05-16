package com.grace.platform.distribution;

import com.grace.platform.distribution.application.DistributionApplicationService;
import com.grace.platform.distribution.application.dto.UploadStatusDTO;
import com.grace.platform.distribution.interfaces.rest.DistributionController;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

class DistributionControllerStatusOrderTest {

    @Test
    void getUploadStatusEndpointCallsSyncThenGetInOrder() {
        DistributionApplicationService service = mock(DistributionApplicationService.class);
        UploadStatusDTO dto = new UploadStatusDTO(
                "rec-1", "task-1", "youtube",
                null, 50, null, null, null);
        when(service.getUploadStatus("task-1")).thenReturn(dto);

        DistributionController controller = new DistributionController(service);
        controller.getUploadStatus("task-1");

        InOrder inOrder = inOrder(service);
        inOrder.verify(service).syncUploadStatus("task-1");
        inOrder.verify(service).getUploadStatus("task-1");
    }
}

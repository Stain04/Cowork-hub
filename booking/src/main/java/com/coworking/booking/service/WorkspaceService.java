package com.coworking.booking.service;

import com.coworking.booking.dto.WorkspaceDTO;
import com.coworking.booking.model.Workspace;
import com.coworking.booking.repository.BookingRepository;
import com.coworking.booking.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final BookingRepository bookingRepository;

    public List<WorkspaceDTO> getAvailableWorkspaces(LocalDateTime start, LocalDateTime end) {
        List<Long> bookedIds = bookingRepository.findBookedWorkspaceIdsInPeriod(start, end);

        List<Workspace> available = bookedIds.isEmpty()
                ? workspaceRepository.findAll()
                : workspaceRepository.findByIdNotIn(bookedIds);

        return available.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public Workspace saveWorkspace(Workspace workspace) {
        return workspaceRepository.save(workspace);
    }

    private WorkspaceDTO mapToDTO(Workspace workspace) {
        return WorkspaceDTO.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .type(workspace.getType())
                .pricePerHour(workspace.getPricePerHour())
                .capacity(workspace.getCapacity())
                .available(workspace.isAvailable())
                .description(workspace.getDescription())
                .build();
    }
}
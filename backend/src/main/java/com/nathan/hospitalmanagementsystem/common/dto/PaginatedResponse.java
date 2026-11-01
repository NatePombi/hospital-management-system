package com.nathan.hospitalmanagementsystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> data;
    private int currentPage;
    private int size;
    private int totalPages;
    private long totalItems;
    private boolean first;
    private boolean isLast;
    
    
    public static <T> PaginatedResponse<T> from(Page<T> result){
        return new PaginatedResponse<>(
                List.copyOf(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages(),
                result.getTotalElements(),
                result.isFirst(),
                result.isLast()
        );
    }
}

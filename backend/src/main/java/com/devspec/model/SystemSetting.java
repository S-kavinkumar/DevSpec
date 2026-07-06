package com.devspec.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @Column(name = "key_name")
    private String keyName;

    @Column(name = "value_content", nullable = false, length = 1024)
    private String valueContent;
}

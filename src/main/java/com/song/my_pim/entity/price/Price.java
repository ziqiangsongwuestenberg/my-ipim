package com.song.my_pim.entity.price;

import com.song.my_pim.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(
        name = "price",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_price_client_identifier",
                        columnNames = {"client", "identifier"})
        }
)
public class Price extends BaseEntity {

    @Column(nullable = false)
    private Integer client;

    @Column(nullable = false, length = 100)
    private String identifier;   // LIST_PRICE, SALE_PRICE

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency;     // EUR

    @Column(name = "price_type", nullable = false, length = 50)
    private String priceType;    // LIST, SALE

    @Column(nullable = false)
    private boolean deleted = false;

}

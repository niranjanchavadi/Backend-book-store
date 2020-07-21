package com.bridgelabz.bookstore.model;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Cart")
public class CartModel {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	@Column(unique = true, nullable=false)
	private Long bookId;
	private long quantity;
	private double totalPrice;
	private String name;
	private String author;
	private String imgUrl;
	private boolean isInWishList;
	private long userId;
}

package com.bridgelabz.bookstore.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.stereotype.Component;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookDto
{
	@NotBlank(message="BookName is mandatory")
	 private String bookName;
	@Min(1)
	  private int quantity;
	@Min(1)	
	private Double price;
	@Size(min=2, max=30)
	private String authorName;
		
//		private String image;
	@Size(min=2, max=30)
		private String bookDetails;
		
	}

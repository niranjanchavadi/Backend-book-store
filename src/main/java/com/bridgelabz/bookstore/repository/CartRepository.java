package com.bridgelabz.bookstore.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.bridgelabz.bookstore.model.CartModel;

@Repository
@Transactional
public interface CartRepository extends JpaRepository<CartModel, Long> {

	@Query(value = "select * from Cart where book_id=?", nativeQuery = true)
	Optional<CartModel> findByBookId(Long book_id);

	@Query(value = "select count(*) from cart id",nativeQuery = true)
	int getCountOfBooks();

	@Query(value = "select id from cart where book_id=?", nativeQuery = true)
	Long getCardId(Long bookId);

	CartModel findByUserIdAndBookId(long id, Long bookId);

	void deleteByUserIdAndBookId(long id, Long bookId);

	List<CartModel> findAllByUserId(long userId);

	@Query(value = "select book_id from cart where book_id=?", nativeQuery = true)
	Long findduplicatebookId(Long bookId);
}

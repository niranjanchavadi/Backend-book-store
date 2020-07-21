package com.bridgelabz.bookstore.service;

import com.bridgelabz.bookstore.dto.*;
import com.bridgelabz.bookstore.exception.BookException;
import com.bridgelabz.bookstore.model.BookModel;
import com.bridgelabz.bookstore.model.CartModel;
import com.bridgelabz.bookstore.model.UserDetailsDAO;
import com.bridgelabz.bookstore.response.UserAddressDetailsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import com.bridgelabz.bookstore.exception.UserException;
import com.bridgelabz.bookstore.exception.UserNotFoundException;
import com.bridgelabz.bookstore.model.UserModel;
import com.bridgelabz.bookstore.response.Response;
import com.bridgelabz.bookstore.response.UserDetailsResponse;

import java.util.List;

@Component
public interface UserService {

    boolean register(RegistrationDto registrationDto) throws UserException;

    boolean verify(String token);

	UserDetailsResponse forgetPassword(ForgotPasswordDto emailId);

    boolean resetPassword(ResetPasswordDto resetPassword, String token) throws UserNotFoundException;

    Response login(LoginDto logindto) throws UserNotFoundException, UserException;


    Response addToCart(Long bookId, String token) throws BookException;

    Response addMoreItems(Long bookId, String token) throws BookException, UserException;

    Response removeItem(Long bookId, String token) throws BookException, UserException;

     void removebookItem(Long bookId, String token) throws BookException, UserException;

    List<CartModel> getAllItemFromCart(@RequestParam String token) throws BookException;

    List<BookModel> sortBookByAsc();

	List<BookModel> sortBookByDesc();

    int getStoredBookCount(int... attribute);

	List<BookModel> getAllBooks() throws UserException;

	BookModel getBookDetails(Long bookId) throws UserException;

    UserAddressDetailsResponse getUserDetails(long userId);


    Response addUserDetails(UserDetailsDTO userDetail, String token);

    Response deleteUserDetails(UserDetailsDTO userDetail, long userId);

    List<BookModel> findByAuthorName(String authorname);

    List<BookModel> findByTitle(String title);

    long getOrderId();

    Response addToWishList(Long bookId, String token) throws BookException;

    Response deleteFromWishlist(Long bookId, String token);

    Response addFromWishlistToCart(Long bookId, String token);

    List<CartModel> getAllItemFromWishList(String token) throws BookException;

}

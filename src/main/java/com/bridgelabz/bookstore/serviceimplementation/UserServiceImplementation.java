package com.bridgelabz.bookstore.serviceimplementation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.bridgelabz.bookstore.dto.*;
import com.bridgelabz.bookstore.exception.BookException;
import com.bridgelabz.bookstore.model.*;
import com.bridgelabz.bookstore.repository.*;
import com.bridgelabz.bookstore.response.UserAddressDetailsResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.bridgelabz.bookstore.exception.UserException;
import com.bridgelabz.bookstore.exception.UserNotFoundException;
import com.bridgelabz.bookstore.exception.UserVerificationException;
import com.bridgelabz.bookstore.response.Response;
import com.bridgelabz.bookstore.response.UserDetailsResponse;
import com.bridgelabz.bookstore.service.UserService;
import com.bridgelabz.bookstore.utility.JavaMailservices;
import com.bridgelabz.bookstore.utility.JwtGenerator;
import com.bridgelabz.bookstore.utility.RedisTempl;
import com.google.common.base.Optional;

import static java.util.stream.Collectors.toList;

@Service
@PropertySource(name = "user", value = {"classpath:response.properties"})
public class UserServiceImplementation implements UserService {
    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
   	private JavaMailservices messageService;

    @Autowired
    private RedisTempl<Object> redis;

    private String redisKey = "Key";

    private static final long REGISTRATION_EXP = (long) 1080000000;
    private static final String VERIFICATION_URL = "http://localhost:8080/user/verify/";
    private static final String RESETPASSWORD_URL = "http://localhost:8080/user/resetpassword?token=";

    @Override
    public boolean register(RegistrationDto registrationDto) throws UserException {
        UserModel emailavailable = userRepository.findByEmailId(registrationDto.getEmailId());
        if (emailavailable != null) {
            return false;
        } else {
            UserModel userDetails = new UserModel();
            BeanUtils.copyProperties(registrationDto, userDetails);
            userDetails.setPassword(bCryptPasswordEncoder.encode(userDetails.getPassword()));
            long id = userRepository.save(userDetails).getUserId();
            UserModel sendMail = userRepository.findByEmailId(registrationDto.getEmailId());
            String response = VERIFICATION_URL + JwtGenerator.createJWT(sendMail.getUserId(), REGISTRATION_EXP);
            redis.putMap(redisKey, userDetails.getEmailId(), userDetails.getFullName());
            switch (registrationDto.getRoleType()) {
                case SELLER:
                    SellerModel sellerDetails = new SellerModel();
                    sellerDetails.setSellerName(registrationDto.getFullName());
                    sellerDetails.setEmailId(registrationDto.getEmailId());
                    sellerDetails.setUserId(id);
                    sellerRepository.save(sellerDetails);
                    break;
                case ADMIN:
                    AdminModel adminDetails = new AdminModel();
                    adminDetails.setAdminName(registrationDto.getFullName());
                    adminDetails.setEmailId(registrationDto.getEmailId());
                    adminRepository.save(adminDetails);
                    break;
            }
            if (messageService.send(sendMail.getEmailId(), "Registration Link...", response))
                return true;

        }
        throw new UserException(environment.getProperty("user.invalidcredentials"), HttpStatus.FORBIDDEN);
    }

    @Override
    public boolean verify(String token) {
        long id = JwtGenerator.decodeJWT(token);
        UserModel userInfo = userRepository.findByUserId(id);
        if (id > 0 && userInfo != null) {
            if (!userInfo.isVerified()) {
                userInfo.setVerified(true);
                userInfo.setUpdatedAt(LocalDateTime.now());
                userRepository.save(userInfo);
                return true;
            }
            throw new UserVerificationException(HttpStatus.CREATED.value(),
                    environment.getProperty("user.already.verified"));
        }
        return false;
    }

    @Override
    public UserDetailsResponse forgetPassword(ForgotPasswordDto userMail) {
        UserModel isIdAvailable = userRepository.findByEmailId(userMail.getEmailId());
        if (isIdAvailable != null && isIdAvailable.isVerified()) {
            String token = JwtGenerator.createJWT(isIdAvailable.getUserId(), REGISTRATION_EXP);
            String response = RESETPASSWORD_URL + token;
            if (messageService.send(isIdAvailable.getEmailId(), "ResetPassword Link...", response))
                return new UserDetailsResponse(HttpStatus.OK.value(), "ResetPassword link Successfully", token);
        }
        return new UserDetailsResponse(HttpStatus.OK.value(), "Eamil ending failed");
    }

    @Override
    public boolean resetPassword(ResetPasswordDto resetPassword, String token) throws UserNotFoundException {
        if (resetPassword.getNewPassword().equals(resetPassword.getConfirmPassword())) {
            long id = JwtGenerator.decodeJWT(token);
            UserModel isIdAvailable = userRepository.findByUserId(id);
            if (isIdAvailable != null) {
                isIdAvailable.setPassword(bCryptPasswordEncoder.encode((resetPassword.getNewPassword())));
                userRepository.save(isIdAvailable);
                redis.putMap(redisKey, resetPassword.getNewPassword(), token);
                return true;
            }
            throw new UserNotFoundException(environment.getProperty("user.not.exist"));
        }
        return false;
    }

    @Override
    public Response login(LoginDto loginDTO) throws UserNotFoundException, UserException {
        UserModel userCheck = userRepository.findByEmailId(loginDTO.getEmailId());

        if (userCheck == null) {
            throw new UserNotFoundException("user.not.exist");
        }
        if (bCryptPasswordEncoder.matches(loginDTO.getPassword(), userCheck.getPassword())) {

            String token = JwtGenerator.createJWT(userCheck.getUserId(), REGISTRATION_EXP);

            redis.putMap(redisKey, userCheck.getEmailId(), userCheck.getPassword());
            userCheck.setUserStatus(true);
            userRepository.save(userCheck);
            return new Response(HttpStatus.OK.value(), token);
        }

        throw new UserException(environment.getProperty("user.invalid.credential"));

    }


    @Override
    public Response addToCart(Long bookId,String token) throws BookException {
        BookModel bookModel = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookException(environment.getProperty("book.not.exist"),HttpStatus.NOT_FOUND));
        Long id = JwtGenerator.decodeJWT(token);
        Long bookid = cartRepository.findduplicatebookId(bookId);
        String role = userRepository.checkRole(id);
        if(bookid!=bookId) {
        if (bookModel.isVerfied()&&role.equals("USER")) {
            CartModel cartModel = new CartModel();
            cartModel.setBookId(bookId);
            cartModel.setTotalPrice(bookModel.getPrice());
            cartModel.setQuantity(1);
            cartModel.setAuthor(bookModel.getAuthorName());
            cartModel.setName(bookModel.getBookName());
            cartModel.setImgUrl(bookModel.getImageUrl());
            cartModel.setUserId(id);
            cartRepository.save(cartModel);
            return new Response(environment.getProperty("book.added.to.cart.successfully"), HttpStatus.OK.value(), cartModel);
        }
        throw new BookException(environment.getProperty("book.unverified"), HttpStatus.OK);
        }
        throw new BookException(environment.getProperty("book.already.added"), HttpStatus.OK);

    }

    @Override
    public Response addMoreItems(Long bookId,String token) throws BookException, UserException {
    	Long id = JwtGenerator.decodeJWT(token);
		String role = userRepository.checkRole(id);
		if (role.equals("USER")) {
        CartModel cartModel = cartRepository.findByBookId(bookId)
                .orElseThrow(() -> new BookException(environment.getProperty("book.not.added"), HttpStatus.NOT_FOUND));
         BookModel quantitty = bookRepository.findByBookId(bookId) ;    
        long quantity = cartModel.getQuantity();
        if(quantity<quantitty.getQuantity()) {
        cartModel.setTotalPrice(cartModel.getTotalPrice() * (quantity + 1) / quantity);
        quantity++;
        cartModel.setQuantity(quantity);
        cartRepository.save(cartModel);
        }
        else {
        	throw new UserException(environment.getProperty("book.outofStock.status"));
        }       	
        return new Response(environment.getProperty("book.added.to.cart.successfully"), HttpStatus.OK.value(), cartModel);
		} else {
			throw new UserException(environment.getProperty("book.unauthorised.status"));
		}
    }

    @Override
    public Response removeItem(Long bookId,String token) throws BookException, UserException {
    	Long id = JwtGenerator.decodeJWT(token);
		String role = userRepository.checkRole(id);
		if (role.equals("USER")) {
        CartModel cartModel = cartRepository.findByBookId(bookId)
                .orElseThrow(() -> new BookException(environment.getProperty("book.not.added"), HttpStatus.NOT_FOUND));
        long quantity = cartModel.getQuantity();
        if (quantity == 1) {
           
            return new Response(HttpStatus.OK.value(), environment.getProperty("items.should.not"));
        }
        cartModel.setTotalPrice(cartModel.getTotalPrice() * (quantity - 1) / quantity);
        quantity--;
        cartModel.setQuantity(quantity);
        cartRepository.save(cartModel);
        return new Response(environment.getProperty("one.quantity.removed.success"), HttpStatus.OK.value(), cartModel);

		} else {
			throw new UserException(environment.getProperty("book.unauthorised.status"));
		}

    }


    @Override
    public void removebookItem(Long bookId,String token) throws BookException, UserException {
    	Long id = JwtGenerator.decodeJWT(token);
		String role = userRepository.checkRole(id);
		if (role.equals("USER")) {
        Long cartId =  cartRepository.getCardId(bookId);
        cartRepository.deleteById(cartId);
		} else {
			throw new UserException(environment.getProperty("book.unauthorised.status"));
		}
    }
    
    @Override
    public List<CartModel> getAllItemFromCart(String token) throws BookException {
        Long id = JwtGenerator.decodeJWT(token);
        List<CartModel> items = cartRepository.findAllByUserId(id).stream().filter(c -> !c.isInWishList()).collect(Collectors.toList());
        if (items.isEmpty())
            return new ArrayList<>();
        return items;
    }

    @Override
    public List<BookModel> sortBookByAsc() {
        return bookRepository.sortBookAsc();
    }

    @Override
    public List<BookModel> sortBookByDesc() {
        return bookRepository.sortBookDesc();
    }


    @Override
    public List<BookModel> getAllBooks() throws UserException
    {
        List<BookModel> booklist=bookRepository.getAllBooks();
        return booklist;
    }

    @Override
    public BookModel getBookDetails(Long bookid) throws UserException
    {
        BookModel bookdetail=bookRepository.getBookDetail(bookid);
        return bookdetail;
    }




    /************************ user details ****************************/
    @Override
    public UserAddressDetailsResponse getUserDetails(long userId) {
        UserModel user = userRepository.findByUserId(userId);
        List<UserDetailsDTO> allDetailsByUser = user.getListOfUserDetails().stream().map(this::mapData).collect(toList());
        if (allDetailsByUser.isEmpty())
            return new UserAddressDetailsResponse(HttpStatus.OK.value(), environment.getProperty("user.details.nonAvailable"));
        return new UserAddressDetailsResponse(HttpStatus.OK.value(), environment.getProperty("user.details.available"), allDetailsByUser);
    }

    private UserDetailsDTO mapData(UserDetailsDAO details) {
        UserDetailsDTO userDto = new UserDetailsDTO();
        BeanUtils.copyProperties(details, userDto);
        return userDto;
    }

    @Override
    public Response addUserDetails(UserDetailsDTO userDetail, String token) {
        Long userId = JwtGenerator.decodeJWT(token);
        UserDetailsDAO userDetailsDAO = new UserDetailsDAO();
        BeanUtils.copyProperties(userDetail, userDetailsDAO);
        UserModel user = userRepository.findByUserId(userId);
        userDetailsDAO.setUserId(userId);
        user.addUserDetails(userDetailsDAO);
        userRepository.save(user);
        userDetailsDAO.setUser(user);
        userDetailsRepository.save(userDetailsDAO);
        return new Response(HttpStatus.OK.value(), environment.getProperty("user.details.added"));
    }

    @Override
    public Response deleteUserDetails(UserDetailsDTO userDetail, long userId) {
        UserModel userModel = userRepository.findByUserId(userId);
        UserDetailsDAO userDetailsDAO = userDetailsRepository.findByAddressAndUserId(userDetail.getAddress(), userId);
        userModel.removeUserDetails(userDetailsDAO);
        userDetailsRepository.delete(userDetailsDAO);
        userRepository.save(userModel);
        return new Response(HttpStatus.OK.value(), environment.getProperty("user.details.deleted"));
    }

    @Override
    public int getStoredBookCount(int... attribute) {
        if(attribute.length > 0){
            return cartRepository.getCountOfBooks();
        }
        return 0;
    }


    @Override
    public List<BookModel> findByAuthorName(String authorname) {
        List<BookModel> bookes = bookRepository.findAll();
        List<BookModel> list = bookes.stream().filter(note -> note.getAuthorName().contains(authorname))
                .collect(Collectors.toList());
        return list;

    }


    @Override
    public List<BookModel> findByTitle(String title) {
        List<BookModel> bookes = bookRepository.findAll();
        List<BookModel> list1 = bookes.stream().filter(note -> note.getBookName().contains(title))
                .collect(Collectors.toList());
        return list1;
    }

    @Override
    public long getOrderId() {

            return (long) (Math.random() * 45678) + 999999;
        }

    @Override
    public Response addToWishList(Long bookId, String token) throws BookException {
        long id = JwtGenerator.decodeJWT(token);
        Long bookid = cartRepository.findduplicatebookId(bookId);
        String role = userRepository.checkRole(id);
        if(bookid!=bookId&&role.equals("USER")) {
        CartModel cartData = cartRepository.findByUserIdAndBookId(id, bookId);
        if (cartData != null && cartData.isInWishList()) {
            return new Response(HttpStatus.OK.value(), "Book already present in wishlist");
        } else if (cartData != null && !cartData.isInWishList()) {
            return new Response(HttpStatus.OK.value(), "Book already added to Cart");
        } else {
            BookModel bookModel = bookRepository.findByBookId(bookId);
            CartModel cartModel = new CartModel();
            BeanUtils.copyProperties(bookModel, cartModel);
            cartModel.setQuantity(1);
            cartModel.setUserId(JwtGenerator.decodeJWT(token));
            cartModel.setInWishList(true);
            cartModel.setBookId(bookId);
            cartModel.setTotalPrice(bookModel.getPrice());
            cartModel.setQuantity(1);
            cartModel.setAuthor(bookModel.getAuthorName());
            cartModel.setName(bookModel.getBookName());
            cartModel.setImgUrl(bookModel.getImageUrl());
            cartRepository.save(cartModel);
            return new Response(HttpStatus.OK.value(), "Book added to WishList");
        }
        }
        throw new BookException(environment.getProperty("book.already.added.wishlist"), HttpStatus.OK);
    }

    @Override
    public Response deleteFromWishlist(Long bookId, String token) {
        long id = JwtGenerator.decodeJWT(token);
        cartRepository.deleteByUserIdAndBookId(id, bookId);
        return new Response(HttpStatus.OK.value(), "Removed SuccessFully from WishKart");
    }

    @Override
    public Response addFromWishlistToCart(Long bookId, String token) {
        long id = JwtGenerator.decodeJWT(token);
        CartModel cartModel = cartRepository.findByUserIdAndBookId(id, bookId);
        if(cartModel.isInWishList()){
            cartModel.setInWishList(false);
            cartRepository.save(cartModel);
            return new Response(HttpStatus.OK.value(), "Added SuccessFully To addToKart from wishlist");
        }
        return new Response(HttpStatus.OK.value(), "Already present in cart, ready to checkout");
    }

    @Override
    public List<CartModel> getAllItemFromWishList(String token) throws BookException {
        Long id = JwtGenerator.decodeJWT(token);
        List<CartModel> items = cartRepository.findAllByUserId(id).stream().filter(CartModel::isInWishList).collect(Collectors.toList());
        if (items.isEmpty())
            return new ArrayList<>();
        return items;
    }

}

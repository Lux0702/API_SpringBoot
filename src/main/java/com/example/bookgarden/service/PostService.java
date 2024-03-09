package com.example.bookgarden.service;

import com.example.bookgarden.constant.OrderStatus;
import com.example.bookgarden.dto.*;
import com.example.bookgarden.entity.*;
import com.example.bookgarden.repository.*;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CommentService commentService;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private BookDetailRepository bookDetailRepository;
    public ResponseEntity<GenericResponse> createPost(String userId, PostCreateRequestDTO postCreateRequestDTO) {
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Không tìm thấy người dùng")
                        .data(null)
                        .build());
            }
            Post newPost = new Post();
            newPost.setTitle(postCreateRequestDTO.getTitle());
            newPost.setContent(postCreateRequestDTO.getContent());
            newPost.setPostedBy(new ObjectId(userId));
            newPost.setBook(new ObjectId(postCreateRequestDTO.getBookId()));
            newPost.setStatus("Pending");

            Post savedPost = postRepository.save(newPost);
            PostResponseDTO postResponseDTO = convertPostToDTO(savedPost);
            return ResponseEntity.status(HttpStatus.CREATED).body(GenericResponse.builder()
                    .success(true)
                    .message("Bài viết đã được tạo thành công")
                    .data(postResponseDTO)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(GenericResponse.builder()
                    .success(false)
                    .message("Lỗi khi tạo bài viết")
                    .data(e.getMessage())
                    .build());
        }
    }

    public ResponseEntity<GenericResponse> getAllApprovedPosts() {
        try {
            Sort sortByPostedDateDesc = Sort.by(Sort.Direction.DESC, "postedDate");

            List<Post> approvedPosts = postRepository.findAllByStatus("Approved", sortByPostedDateDesc);

            List<PostResponseDTO> postResponseDTOs = approvedPosts.stream()
                    .map(this::convertPostToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(GenericResponse.builder()
                    .success(true)
                    .message("Lấy danh sách bài viết đã phê duyệt thành công")
                    .data(postResponseDTOs)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(GenericResponse.builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách bài viết đã phê duyệt")
                    .data(e.getMessage())
                    .build());
        }
    }

    public ResponseEntity<GenericResponse> getPostById(String postId) {
        try {
            Optional<Post> optionalPost = postRepository.findById(new ObjectId(postId));
            if (optionalPost.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Không tìm thấy bài viết")
                        .data(null)
                        .build());
            }
            PostResponseDTO postResponseDTO = convertPostToDTO(optionalPost.get());

            return ResponseEntity.ok(GenericResponse.builder()
                    .success(true)
                    .message("Lấy danh sách bài viết đã phê duyệt thành công")
                    .data(postResponseDTO)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(GenericResponse.builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách bài viết đã phê duyệt")
                    .data(e.getMessage())
                    .build());
        }
    }

    public ResponseEntity<GenericResponse> updatePostStatus(String userId, String postId, UpdatePostStatusRequestDTO updatePostStatusRequestDTO) {
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isPresent()) {
                if (!("Admin".equals(optionalUser.get().getRole()))) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(GenericResponse.builder()
                            .success(false)
                            .message("Bạn không có quyền đổi trạng thái bài viết")
                            .data(null)
                            .build());
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Người dùng không tồn tại")
                        .data(null)
                        .build());
            }
            Optional<Post> optionalPost = postRepository.findById(new ObjectId(postId));
            if (optionalPost.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Không tìm thấy bài viết")
                        .data(null)
                        .build());
            }
            Post post = optionalPost.get();
            String newStatus = updatePostStatusRequestDTO.getStatus();
            if (!canUpdatePostStatus(post.getStatus(), newStatus)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GenericResponse.builder()
                        .success(false)
                        .message("Không thể thay đổi trạng thái đơn hàng")
                        .data(newStatus)
                        .build());
            }
            post.setStatus(newStatus);
            post = postRepository.save(post);
            PostResponseDTO postResponseDTO = convertPostToDTO(post);
            return ResponseEntity.ok(GenericResponse.builder()
                    .success(true)
                    .message("Phê duyệt bài viết thành công")
                    .data(postResponseDTO)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(GenericResponse.builder()
                    .success(false)
                    .message("Lỗi khi phê duyệt bài viết")
                    .data(e.getMessage())
                    .build());
        }
    }

    public ResponseEntity<GenericResponse> getAllPosts(String userId) {
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isPresent()) {
                if (!("Admin".equals(optionalUser.get().getRole()))) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(GenericResponse.builder()
                            .success(false)
                            .message("Bạn không có quyền lấy danh sách bài viết")
                            .data(null)
                            .build());
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Người dùng không tồn tại")
                        .data(null)
                        .build());
            }
            Sort sortByPostedDateDesc = Sort.by(Sort.Direction.DESC, "postedDate");

            List<Post> approvedPosts = postRepository.findAll(sortByPostedDateDesc);

            List<PostResponseDTO> postResponseDTOs = approvedPosts.stream()
                    .map(this::convertPostToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(GenericResponse.builder()
                    .success(true)
                    .message("Lấy danh sách bài viết thành công")
                    .data(postResponseDTOs)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(GenericResponse.builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách bài viết")
                    .data(e.getMessage())
                    .build());
        }
    }

    public ResponseEntity<GenericResponse> getUserPosts(String userId) {
        try{
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Không tìm thấy người dùng")
                        .data(null)
                        .build());
            }
            Sort sortByPostedDateDesc = Sort.by(Sort.Direction.DESC, "postedDate");

            List<Post> approvedPosts = postRepository.findAllByPostedBy(new ObjectId(userId), sortByPostedDateDesc);

            List<PostResponseDTO> postResponseDTOs = approvedPosts.stream()
                    .map(this::convertPostToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(GenericResponse.builder()
                    .success(true)
                    .message("Lấy danh sách bài viết thành công")
                    .data(postResponseDTOs)
                    .build());
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(GenericResponse.builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách bài viết")
                    .data(e.getMessage())
                    .build());
        }
    }

    public ResponseEntity<GenericResponse> commentPost(String userId, String postId, CommentRequestDTO commentRequestDTO) {
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            Optional<Post> optionalPost = postRepository.findById(new ObjectId(postId));

            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Không tìm thấy người dùng")
                        .data(null)
                        .build());
            }

            if (optionalPost.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Không tìm thấy bài viết")
                        .data(null)
                        .build());
            }

            User user = optionalUser.get();
            Post post = optionalPost.get();

            Comment comment = new Comment();
            comment.setUser(new ObjectId(user.getId()));
            comment.setPost(post.getId());
            comment.setComment(commentRequestDTO.getComment());
            comment.setCreatedDate(new Date());
            comment = commentRepository.save(comment);

            List<ObjectId> comments = post.getComments();
            if (comments == null) {
                comments = new ArrayList<>();
            }
            comments.add(comment.getId());
            post.setComments(comments);
            postRepository.save(post);

            PostResponseDTO postResponseDTO = convertPostToDTO(post);
            return ResponseEntity.ok(GenericResponse.builder()
                    .success(true)
                    .message("Bình luận bài viết thành công")
                    .data(postResponseDTO)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(GenericResponse.builder()
                    .success(false)
                    .message("Lỗi khi bình luận bài viết")
                    .data(e.getMessage())
                    .build());
        }
    }

    public ResponseEntity<GenericResponse> replyPostComment(String userId, String postId, String commentId, CommentRequestDTO commentRequestDTO) {
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Không tìm thấy người dùng")
                        .data(null)
                        .build());
            }
            User user = optionalUser.get();

            Optional<Post> optionalPost = postRepository.findById(new ObjectId(postId));
            if (optionalPost.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Không tìm thấy bài viết")
                        .data(null)
                        .build());
            }
            Post post = optionalPost.get();

            Optional<Comment> optionalComment = commentRepository.findById(new ObjectId(commentId));
            if (optionalComment.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.builder()
                        .success(false)
                        .message("Không tìm thấy bình luận")
                        .data(null)
                        .build());
            }
            Comment comment = optionalComment.get();

            // Kiểm tra và tạo danh sách trả lời nếu cần
            List<ObjectId> replies = comment.getReplies();
            if (replies == null) {
                replies = new ArrayList<>();
            }

            // Tạo bình luận trả lời
            Comment reply = new Comment();
            reply.setUser(new ObjectId(user.getId()));
            reply.setPost(post.getId());
            reply.setComment(commentRequestDTO.getComment());
            reply.setCreatedDate(new Date());
            reply = commentRepository.save(reply);

            // Thêm bình luận trả lời vào danh sách trả lời và cập nhật bình luận chính
            replies.add(reply.getId());
            comment.setReplies(replies);
            commentRepository.save(comment);

            // Cập nhật thông tin bài viết và trả về kết quả
            postRepository.save(post);
            PostResponseDTO postResponseDTO = convertPostToDTO(post);
            return ResponseEntity.ok(GenericResponse.builder()
                    .success(true)
                    .message("Bình luận bài viết thành công")
                    .data(postResponseDTO)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(GenericResponse.builder()
                    .success(false)
                    .message("Lỗi khi bình luận bài viết")
                    .data(e.getMessage())
                    .build());
        }
    }

    private PostResponseDTO convertPostToDTO(Post post){
        ModelMapper modelMapper = new ModelMapper();
        PostResponseDTO postResponseDTO = modelMapper.map(post, PostResponseDTO.class);
        Optional<User> optionalUser = userRepository.findById(post.getPostedBy().toString());
        if(optionalUser.isPresent()){
            UserPostDTO userPostDTO = modelMapper.map(optionalUser.get(), UserPostDTO.class);
            postResponseDTO.setPostedBy(userPostDTO);
        }
        Optional<Book> optionalBook = bookRepository.findById(post.getBook());
        if(optionalBook.isPresent()){
            BookPostDTO bookPostDTO = modelMapper.map(optionalBook.get(), BookPostDTO.class);
            bookDetailRepository.findByBook(optionalBook.get().getId())
                    .ifPresent(bookDetail -> bookPostDTO.setImage(bookDetail.getImage()));

            postResponseDTO.setBook(bookPostDTO);
        }
        System.out.println(optionalBook);
        List<ObjectId> commentIds = post.getComments();
        System.out.println(commentIds);
        List<Comment> comments = commentRepository.findAllByIdIn(commentIds);
        List<CommentDTO> commentDTOs = comments.stream()
                .map(comment -> commentService.convertCommentToDTO(comment))
                .collect(Collectors.toList());
        postResponseDTO.setComments(commentDTOs);
        return postResponseDTO;
    }

    private boolean canUpdatePostStatus(String currentStatus, String newStatus) {
        switch (currentStatus) {
            case "Pending":
                return "Approved".equals(newStatus) || "Rejected".equals(newStatus);
            default:
                return false;
        }
    }

}

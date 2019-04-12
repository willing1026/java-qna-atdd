package nextstep.service;

import nextstep.UnAuthorizedException;
import nextstep.domain.entity.Answer;
import nextstep.domain.entity.Question;
import nextstep.domain.entity.User;
import nextstep.domain.repository.AnswerRepository;
import nextstep.domain.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service("qnaService")
public class QnaService {
    private static final Logger log = LoggerFactory.getLogger(QnaService.class);

    @Resource(name = "questionRepository")
    private QuestionRepository questionRepository;

    @Resource(name = "answerRepository")
    private AnswerRepository answerRepository;

    @Resource(name = "deleteHistoryService")
    private DeleteHistoryService deleteHistoryService;

    public Question create(User loginUser, Question question) {
        question.writeBy(loginUser);
        log.debug("question : {}", question);
        return questionRepository.save(question);
    }

    public Optional<Question> findQuestionById(long id) {
        return questionRepository.findById(id);
    }

    public Optional<Answer> findAnswerById(long id) {
        return answerRepository.findById(id);
    }


    public Question show(long id) {
        return findQuestionById(id).orElseThrow(UnAuthorizedException::new);
    }

    public Question findQuestion(long id, User loginUser) {
        return findQuestionById(id)
                .filter(question -> question.isOwner(loginUser))
                .orElseThrow(UnAuthorizedException::new);
    }

    @Transactional
    public Question update(User loginUser, long id, Question updatedQuestion) {
        return findQuestionById(id)
                .map(question -> question.modify(loginUser, updatedQuestion))
                .orElseThrow(UnAuthorizedException::new);
    }

    @Transactional
    public Question deleteQuestion(User loginUser, long id) {
        Question deleteQuestion = findQuestionById(id)
                .map(question -> question.delete(loginUser))
                .orElseThrow(UnAuthorizedException::new);

        deleteHistoryService.saveAll(deleteQuestion);

        return findQuestionById(id)
                .map(question -> question.delete(loginUser))
                .orElseThrow(UnAuthorizedException::new);
    }

    public Iterable<Question> findAll() {
        return questionRepository.findByDeleted(false);
    }

    public List<Question> findAll(Pageable pageable) {
        return questionRepository.findAll(pageable).getContent();
    }

    @Transactional
    public Answer addAnswer(User loginUser, long questionId, String contents) {
        Answer answer = new Answer(loginUser, contents);
        findQuestionById(questionId).orElseThrow(UnAuthorizedException::new)
                .addAnswer(answer);

        return answerRepository.save(answer);
    }

    @Transactional(readOnly = true)
    public Answer showAnswer(long answerId) {
        return findAnswerById(answerId)
                .orElseThrow(UnAuthorizedException::new);
    }

    @Transactional
    public Answer deleteAnswer(User loginUser, long id) {
        return findAnswerById(id).orElseThrow(UnAuthorizedException::new)
                .delete(loginUser);
    }

    @Transactional
    public Answer updateAnswer(User loginUser, long answerId, String contents) {
        return findAnswerById(answerId)
                .map(answer -> answer.update(loginUser, contents))
                .orElseThrow(UnAuthorizedException::new);
    }
}

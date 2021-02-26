package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Question;
import play.db.ebean.EbeanConfig;

public class QuestionRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public QuestionRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
  }

  public CompletionStage<Set<Question>> listQuestions() {
    return supplyAsync(() -> ebeanServer.find(Question.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Question>> lookupQuestion(long id) {
    return supplyAsync(
        () -> Optional.ofNullable(ebeanServer.find(Question.class).setId(id).findOne()),
        executionContext);
  }

  static class PathConflictDetector {
    private Optional<Question> conflictedQuestion = Optional.empty();
    private final String newPath;

    PathConflictDetector(String newPath) {
      this.newPath = checkNotNull(newPath);
    }

    public Optional<Question> getConflictedQuestion() {
      return conflictedQuestion;
    }

    public boolean checkConflict(Question question) {
      boolean proceed = true;
      if (pathConflicts(question.getPath(), newPath)) {
        conflictedQuestion = Optional.of(question);
        proceed = false;
      }
      return proceed;
    }

    public static boolean pathConflicts(String path, String otherPath) {
      path = path.toLowerCase();
      otherPath = otherPath.toLowerCase();
      return path.startsWith(otherPath) || otherPath.startsWith(path);
    }
  }

  public CompletionStage<Optional<Question>> findConflictingQuestion(String newPath) {
    return supplyAsync(
        () -> {
          PathConflictDetector detector = new PathConflictDetector(newPath);
          ebeanServer.find(Question.class).findEachWhile(detector::checkConflict);
          return detector.getConflictedQuestion();
        },
        executionContext);
  }

  public CompletionStage<Optional<Question>> lookupQuestionByPath(String path) {
    return supplyAsync(
        () ->
            Optional.ofNullable(
                ebeanServer.find(Question.class).where().eq("path", path).findOne()),
        executionContext);
  }

  public CompletionStage<Question> insertQuestion(Question question) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(question);
          return question;
        },
        executionContext);
  }

  public Question insertQuestionSync(Question question) {
    ebeanServer.insert(question);
    return question;
  }
}

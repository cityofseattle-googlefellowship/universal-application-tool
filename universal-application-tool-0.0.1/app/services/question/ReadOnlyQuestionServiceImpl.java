package services.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;

public final class ReadOnlyQuestionServiceImpl implements ReadOnlyQuestionService {
  private final ImmutableMap<String, ScalarType> scalars;
  private final ImmutableMap<String, QuestionDefinition> questions;
  private final ImmutableMap<long, QuestionDefinition> questionIds;
  private final ImmutableMap<String, QuestionDefinition> scalarParents;

  private Locale preferredLocale = Locale.ENGLISH;
  private long nextId = 0;

  public ReadOnlyQuestionServiceImpl(ImmutableList<QuestionDefinition> questions) {
    ImmutableMap.Builder<String, QuestionDefinition> questionMap = ImmutableMap.builder();
    ImmutableMap.Builder<long, QuestionDefinition> questionIdsMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, ScalarType> scalarMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, QuestionDefinition> scalarParentsMap = ImmutableMap.builder();
    for (QuestionDefinition qd : questions) {
      String questionPath = qd.getPath();
      // Remove this code.
      long questionId = qd.getId();
      if (questionId >= nextId) { 
        nextId = questionId + 1;
      }
      // End code block to remove.
      questionMap.put(questionPath, qd);
      questionIdsMap.put(qd.getId(), qd);
      ImmutableMap<String, ScalarType> questionScalars = qd.getScalars();
      questionScalars.entrySet().stream()
          .forEach(
              entry -> {
                String fullPath = questionPath + '.' + entry.getKey();
                try {
                  scalarMap.put(fullPath, entry.getValue());
                  scalarParentsMap.put(fullPath, qd);
                } catch (Exception e) {
                  System.out.println("Key already exists: " + fullPath);
                }                
              });
    }
    this.questions = questionMap.build();
    this.questionIds = questionIdsMap.build();
    this.scalars = scalarMap.build();
    this.scalarParents = scalarParentsMap.build();
  }

  public ImmutableList<QuestionDefinition> getAllQuestions() {
    return questions.values().asList();
  }

  public ImmutableMap<String, ScalarType> getAllScalars() {
    return scalars;
  }

  public long getNextId() {
    return nextId++;
  }

  public ImmutableMap<String, ScalarType> getPathScalars(String pathString)
      throws InvalidPathException {
    PathType pathType = this.getPathType(pathString);
    switch (pathType) {
      case QUESTION:
        return questions.get(pathString).getFullyQualifiedScalars();
      case SCALAR:
        ScalarType scalarType = scalars.get(pathString);
        return ImmutableMap.of(pathString, scalarType);
      case NONE:
      default:
        throw new InvalidPathException(pathString);
    }
  }

  public PathType getPathType(String pathString) {
    if (questions.containsKey(pathString)) {
      return PathType.QUESTION;
    } else if (scalars.containsKey(pathString)) {
      return PathType.SCALAR;
    }
    return PathType.NONE;
  }  

  public QuestionDefinition getQuestionDefinition(long id) throws InvalidPathException {
    QuestionDefinition definition = questionIds.get(id);
    if (definition != null) {
      return definition;
    }
    throw new InvalidPathException("id: " + id);
  }

  public QuestionDefinition getQuestionDefinition(String pathString) throws InvalidPathException {
    PathType pathType = this.getPathType(pathString);
    switch (pathType) {
      case QUESTION:
        return questions.get(pathString);
      case SCALAR:
        return scalarParents.get(pathString);
      case NONE:
      default:
        throw new InvalidPathException(pathString);
    }
  }

  public boolean isValid(String pathString) {
    return scalars.containsKey(pathString) || questions.containsKey(pathString);
  }

  public void setPreferredLocale(Locale locale) {
    this.preferredLocale = locale;
  }
}

package com.t13g2.forum.logic.commands;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;

import com.t13g2.forum.commons.core.Messages;
import com.t13g2.forum.logic.CommandHistory;
import com.t13g2.forum.logic.commands.exceptions.CommandException;
import com.t13g2.forum.logic.util.DisplayFormatter;
import com.t13g2.forum.model.Context;
import com.t13g2.forum.model.Model;
import com.t13g2.forum.model.ModelManager;
import com.t13g2.forum.model.UnitOfWork;
import com.t13g2.forum.model.UserPrefs;
import com.t13g2.forum.model.forum.Comment;
import com.t13g2.forum.model.forum.ForumThread;
import com.t13g2.forum.model.forum.Module;
import com.t13g2.forum.model.forum.User;
import com.t13g2.forum.testutil.CommentBuilder;
import com.t13g2.forum.testutil.ForumThreadBuilder;
import com.t13g2.forum.testutil.TypicalModules;
import com.t13g2.forum.testutil.TypicalPersons;
import com.t13g2.forum.testutil.TypicalUsers;

//@@author HansKoh
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SelectThreadCommandTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private Model model;
    private Model expectedModel;
    private CommandHistory commandHistory = new CommandHistory();

    @Before
    public void setUp() {
        model = new ModelManager(TypicalPersons.getTypicalAddressBook(), new UserPrefs());
        expectedModel = new ModelManager(model.getForumBook(), new UserPrefs());
    }

    @Test
    public void execute_userLoggedInSelectThread_selectThreadSuccess() throws Exception {
        //set the current logged in user as a user.
        User validUser = TypicalUsers.JANEDOE;
        Context.getInstance().setCurrentUser(validUser);

        Module validModule = TypicalModules.CS1231;
        int moduleId = 0;
        int threadId = 0;
        String mCode = validModule.getModuleCode();
        String tTitle = "";
        String message = "";
        try (UnitOfWork unitOfWork = new UnitOfWork()) {
            unitOfWork.getModuleRepository().addModule(validModule);
            moduleId = unitOfWork.getModuleRepository().getModuleByCode(validModule.getModuleCode()).getId();
            ForumThread forumThread = new ForumThreadBuilder().withModuleId(moduleId).build();
            Comment comment = new CommentBuilder().withThreadId(forumThread.getId()).build();

            CreateThreadCommand createThreadCommand =
                    new CreateThreadCommand(validModule.getModuleCode(), forumThread, comment);
            createThreadCommand.execute(model, commandHistory);

            CreateCommentCommand createCommentCommand =
                    new CreateCommentCommand(forumThread.getId(), comment.getContent());
            createCommentCommand.execute(model, commandHistory);
            List<Comment> commentList = unitOfWork.getCommentRepository().getCommentsByThread(forumThread);
            message = DisplayFormatter.displayCommentList(commentList);
            threadId = forumThread.getId();
            tTitle = forumThread.getTitle();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SelectThreadCommand selectThreadCommand = new SelectThreadCommand(threadId);
        String messageSuccess = "Listed all comments under \n"
                + "Module Code: %s\n"
                + "Thread ID      : %s\n"
                + "Thread Title  : %s\n"
                + "****************************************************************************\n"
                + "****************************************************************************\n"
                + "%s";
        CommandTestUtil.assertCommandSuccess(selectThreadCommand, model, commandHistory,
                String.format(messageSuccess, mCode, threadId, tTitle, message), expectedModel);

    }

    @Test
    public void execute_userLoggedInInvalidThreadId_selectThreadFailed() throws Exception {
        //set the current logged in user as a user.
        User validUser = TypicalUsers.JANEDOE;
        Context.getInstance().setCurrentUser(validUser);

        int inValidThreadId = 8888;
        SelectThreadCommand selectThreadCommand = new SelectThreadCommand(inValidThreadId);

        thrown.expect(CommandException.class);
        thrown.expectMessage(Messages.MESSAGE_INVALID_THREAD_ID);

        CommandResult commandResult = selectThreadCommand.execute(model, commandHistory);
        assertEquals(Messages.MESSAGE_INVALID_THREAD_ID, commandResult.feedbackToUser);
    }

    @Test
    public void execute_notLoggedInSelectThread_selectThreadFailed() throws Exception {
        //set the current logged in user as null.
        Context.getInstance().setCurrentUser(null);

        Module validModule = TypicalModules.CS2113;
        ForumThread forumThread = new ForumThreadBuilder().withModuleId(validModule.getId()).build();
        SelectThreadCommand selectThreadCommand = new SelectThreadCommand(forumThread.getId());

        thrown.expect(CommandException.class);
        thrown.expectMessage(Messages.MESSAGE_NOT_LOGIN);

        CommandResult commandResult = selectThreadCommand.execute(model, commandHistory);
        assertEquals(Messages.MESSAGE_NOT_LOGIN, commandResult.feedbackToUser);
    }
}

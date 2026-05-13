Feature: Add button Delete relation

Description:
In current code, On hover to relation node, there will be a option list pop up with some action could choice.

Tasks:
1. Create API DELETE `api/relation-page/{pageId}/{personId}` with personId is node/personId and pageId is current page/relation table id.
2. Implement code to delete person by id;
   - Create new Controller for `api/relation-page/`
   - Verify if personId belong to page/relation page.
   - Processing delete user.
3. Add new button "Delete relation" into option list.
4. On click button "Delete relation", pop up tab to confirm delete action.
5. If user keep delete action, send delete request `api/relation-page/{pageId}/{personId}`.
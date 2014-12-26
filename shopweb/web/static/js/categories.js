(function() {
  $(function() {
    $.blockUI.defaults.baseZ = 90;

    $(".close_dialog").bind("click", function(event) {
      categories.closeDialog();
    });

    categories.refreshList();

  });

})();

var categories = {

  closeDialog : function() {
    $.unblockUI();
  },

  refreshList : function() {

    $(".close_dialog").bind("click", function(event) {
      categories.closeDialog();
    });
    $(".cat_box").each(function(index) {
      var me = $(this);
      var pid = me.attr("id");

      if (window.admin !== undefined) {
        me.find('.edit_tag_close').click(function(event) {
          window.admin.deleteCategory(pid);
          event.preventDefault();
          event.stopPropagation();
        });

        me.find('.edit_tag_update').click(function(event) {
          window.admin.editCategory(pid);
          event.preventDefault();
          event.stopPropagation();
        });

      }

      if (pid !== undefined) {
        me.click(function(event) {
          window.location.href = "/products?cat=" + pid;
          event.preventDefault();
          event.stopPropagation();
        });
      } else {
        if (window.admin !== undefined) {
          window.admin.attachCreateCategory(me);
        }
      }

    });
  },

  reloadCategories : function() {
    $("#item_list").load("/", function(response, status, xhr) {
      if (status == "error") {
        $("#notice_connect_e").show().delay(5000).fadeOut("slow");
      } else {
        categories.refreshList();
      }
    });
  }
}
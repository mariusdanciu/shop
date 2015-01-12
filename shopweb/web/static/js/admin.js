(function() {
  $(function() {
    $('#create_product_tab').tabify();

    $("#update_category_form").keydown(function(event) {
      if (event.keyCode == 13) {
        window.admin.saveCategory("#update_category_form");
        event.stopPropagation();
        event.preventDefault();
        return false;
      }
    });

    $("#create_category_form").keydown(function(event) {
      if (event.keyCode == 13) {
        window.admin.saveCategory("#create_category_form");
        event.stopPropagation();
        event.preventDefault();
        return false;
      }
    });

    $("#create_category").click(function(event) {
      window.admin.saveCategory("#create_category_form");
      event.stopPropagation();
      event.preventDefault();
    });

    $("#update_category").click(function(event) {
      window.admin.saveCategory("#update_category_form");
      event.stopPropagation();
      event.preventDefault();
    });

    $("#create_product").click(function(event) {
      window.admin.save("#upload_form", function() {
        window.products.closeDialog();
        window.products.reloadProducts();
      });
      event.stopPropagation();
      event.preventDefault();
    });

    admin.addProp("#add_prop", "#prop_fields");
    admin.toggleDescription("create");

    document.onkeydown = function(evt) {
      evt = evt || window.event;

      if (evt.keyCode == 27) {
        if (window.products !== undefined) {
          window.products.closeDialog();
        }
        if (window.categories !== undefined) {
          window.common.closeDialog();
        }
      }
    };
  });

})();

var admin = {

  saveCategory : function(formId) {
    window.admin.save(formId, function() {
      window.common.closeDialog();
      window.categories.reloadCategories();
    });
  },

  addProp : function(elem, holder) {
    $(elem).click(function(event) {
      var div = $("<div class='row'></div>");
      div.append("<input type='text' name='pkey'/><input type='text' name='pval'/>");

      var remove = $("<img class='clickable' src='/static/images/minus.png'/>");
      remove.click(function(e) {
        div.remove();
      });

      div.append(remove);
      $(holder).append(div);

    });
  },

  deleteProduct : function(id) {
    $.ajax({
      url : "/product/delete/" + id,
      type : "DELETE"
    }).success(products.reloadProducts()).fail(function(msg, f) {
      $("#notice_connect_e").show().delay(5000).fadeOut("slow");
    });
  },

  deleteCategory : function(id) {
    $.ajax({
      url : "/category/delete/" + id,
      type : "DELETE"
    }).success(categories.reloadCategories()).fail(function(msg, f) {
      $("#notice_connect_e").show().delay(5000).fadeOut("slow");
    });
  },

  save : function(formId, successFunc) {
    $(formId).each(function() {
      var frm = this;
      var formData = new FormData(frm);

      $(formId + ' label').css("color", "#555555").removeAttr("title");
      $.ajax({
        url : $(frm).attr('action'),
        type : "POST",
        cache : false,
        contentType : false,
        processData : false,
        data : formData,
        statusCode : {
          201 : function() {
            successFunc();
          }
        }
      }).fail(function(msg, f) {
        $("#notice_connect_e").show().delay(5000).fadeOut("slow");
      })
    });
  },

  toggleDescription : function(prefix) {
    $("#" + prefix + "_toggle_desc").click(function(e) {
      $("." + prefix + "_toggle_text").toggle();
      $("." + prefix + "_toggle").toggle({
        duration : 0,
        done : function() {
          var preview = $("#" + prefix + "_description_view");
          if (preview.css('display') != 'none') {
            preview.html(textile.convert($("#" + prefix + "_description").val()));
          }
        }
      });
    });

  },

  attachToProduct : function(successFunc) {
    $("#save_product").click(function(event) {
      admin.save("#edit_form", successFunc);
      event.stopPropagation();
      event.preventDefault();
    });
    $('#edit_product_tab').tabify();
    $("#edit_specs .row img").click(function(e) {
      var row = $(this).parent();
      row.remove();
    });
    admin.addProp("#edit_add_prop", "#edit_prop_fields");
    admin.toggleDescription("edit");
  },

  attachCreateProduct : function(elem) {
    elem.click(function(event) {
      $.blockUI({
        message : $("#product_create_dialog"),
        css : {
          top : '70px',
          left : ($(window).width() - 800) / 2 + 'px',
          width : '800px',
          border : 'none',
          cursor : null
        },
        overlayCSS : {
          cursor : null,
          backgroundColor : '#dddddd'
        }
      });
    });
  },

  attachCreateCategory : function(elem) {
    elem.click(function(event) {
      $.blockUI({
        message : $("#category_create_dialog"),
        css : {
          top : '200px',
          left : ($(window).width() - 400) / 2 + 'px',
          width : '400px',
          border : 'none',
          cursor : null
        },
        overlayCSS : {
          cursor : null,
          backgroundColor : '#dddddd'
        }
      });
    });
  },

  editCategory : function(cid) {
    $('#update_category_form').attr("action", "/category/update/" + cid)
    $.blockUI({
      message : $("#category_update_dialog"),
      css : {
        top : '200px',
        left : ($(window).width() - 400) / 2 + 'px',
        width : '400px',
        border : 'none',
        cursor : null
      },
      overlayCSS : {
        cursor : null,
        backgroundColor : '#dddddd'
      }
    });
  }

};
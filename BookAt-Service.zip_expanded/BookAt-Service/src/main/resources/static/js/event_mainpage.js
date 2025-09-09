document.addEventListener("DOMContentLoaded", function () {
  const categoryNav = document.querySelector(".chip-row");
  const links = categoryNav.querySelectorAll("a");

  links.forEach((link) => {
    link.addEventListener("click", function (event) {
      //event.preventDefault();

      links.forEach((innerLink) => innerLink.classList.remove("active"));

      this.classList.add("active");
    });
  });
});

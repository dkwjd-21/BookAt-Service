document.addEventListener("DOMContentLoaded", function () {
  const categoryNav = document.querySelector(".chip-row");
  if (categoryNav) {
    const links = categoryNav.querySelectorAll("a");
    links.forEach((link) => {
      link.addEventListener("click", function () {
        links.forEach((innerLink) => innerLink.classList.remove("active"));
        this.classList.add("active");
      });
    });
  }

  document.querySelectorAll(".event-carousel-section .carousel").forEach(setupCarousel);
});

function setupCarousel(root) {
  const viewport = root.querySelector(".carousel-viewport");
  const track = root.querySelector(".carousel-track");

  const prev = root.previousElementSibling?.classList.contains("carousel-nav--prev") ? root.previousElementSibling : root.querySelector(".carousel-nav--prev");
  const next = root.nextElementSibling?.classList.contains("carousel-nav--next") ? root.nextElementSibling : root.querySelector(".carousel-nav--next");

  if (!viewport || !track || !prev || !next) {
    return;
  }

  const cardWidth = () => track.querySelector(".event-card")?.offsetWidth ?? viewport.clientWidth;

  function updateNav() {
    const maxScroll = track.scrollWidth - viewport.clientWidth;
    prev.disabled = viewport.scrollLeft <= 0;
    next.disabled = viewport.scrollLeft >= maxScroll - 1;
  }

  prev.addEventListener("click", () => {
    viewport.scrollBy({ left: -(cardWidth() * 3), behavior: "smooth" });
    setTimeout(updateNav, 300);
  });

  next.addEventListener("click", () => {
    viewport.scrollBy({ left: cardWidth() * 3, behavior: "smooth" });
    setTimeout(updateNav, 300);
  });

  viewport.addEventListener("scroll", () => {
    window.requestAnimationFrame(updateNav);
  });

  updateNav();
}

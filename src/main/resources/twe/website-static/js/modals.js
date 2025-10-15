class ModalLink extends HTMLElement {
  connectedCallback() {
    this.modalId = this.getAttribute('for')

    const link = document.createElement('a')
    link.classList.add('usa-link')
    link.href = '#' + this.modalId
    link.text = this.innerText
    link.addEventListener('click', (event) => this.onclick(event))

    this.replaceChildren(link)
  }

  onclick(event) {
    event.preventDefault()
    const modal = document.getElementById(this.modalId)
    if (!modal) {
      console.warn(`No modal found for ${this.modalId}`)
    } else {
      const currentVerticalScroll = window.scrollY
      modal.showModal()
      document.body.classList.add("usa-js-modal--active")
      window.scrollTo(0, currentVerticalScroll)
      trapFocus(modal)
    }
  }
}
customElements.define('modal-link', ModalLink)

// Trap focus utility function
function trapFocus(element) {
  const focusableEls = element.querySelectorAll(
    'a[href]:not([disabled]), button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([disabled]):not([tabindex="-1"])'
  );
  const firstFocusableEl = focusableEls[0];
  const lastFocusableEl = focusableEls[focusableEls.length - 1];

  function handleKeyDown(e) {
    const isTabPressed = e.key === 'Tab';

    if (!isTabPressed) {
      return;
    }

    if (e.shiftKey) { // Shift + Tab
      if (document.activeElement === firstFocusableEl) {
        lastFocusableEl.focus();
        e.preventDefault();
      }
    } else { // Tab
      if (document.activeElement === lastFocusableEl) {
        firstFocusableEl.focus();
        e.preventDefault();
      }
    }
  }

  element.addEventListener('keydown', handleKeyDown);

  // Optional: Set initial focus to the first focusable element
  if (firstFocusableEl) {
    firstFocusableEl.focus();
  }

  // Return a function to remove the event listener and release the trap
  return () => {
    element.removeEventListener('keydown', handleKeyDown);
  };
}

const modals = document.querySelectorAll('.usa-modal--dialog');
const closeModalButtons = document.querySelectorAll('[data-close-modal]');

closeModalButtons.forEach((button) => {
  button.addEventListener('click', () => {
    const modal = button.closest('.usa-modal--dialog');
    if (modal) {
      modal.close();
      document.body.classList.remove("usa-js-modal--active");
    }
  });
});

modals.forEach((modal) => {
  modal.addEventListener('close', () => {
    document.body.classList.remove("usa-js-modal--active");
  } );
});

// Close modal when clicking outside the modal content

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import BootstrapToast from 'react-bootstrap/Toast';

export default function Toast(props) {
  const [show, setShow] = useState(true);

  return (
    <BootstrapToast
      style={{ position: 'absolute', top: '1em', right: '1em' }}
      onClose={() => setShow(false)}
      delay={5000}
      show={show}
      autohide
    >
      <BootstrapToast.Header>
        <strong className="mr-auto">{props.header}</strong>
      </BootstrapToast.Header>
      <BootstrapToast.Body>{props.body}</BootstrapToast.Body>
    </BootstrapToast>
  );
}

Toast.propTypes = {
  header: PropTypes.string.isRequired,
  body: PropTypes.string.isRequired,
};

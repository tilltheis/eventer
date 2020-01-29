import React, {useState} from 'react';
import Toast from 'react-bootstrap/Toast';

export default function MyToast(props) {
  const [show, setShow] = useState(true);

  return (
    <Toast style={{ position: 'absolute', top: '2em', right: '2em', }} onClose={() => setShow(false)} delay={5000} show={show} autohide>
      <Toast.Header><strong className="mr-auto">{props.header}</strong></Toast.Header>
      <Toast.Body>{props.body}</Toast.Body>
    </Toast>
  );
};
  